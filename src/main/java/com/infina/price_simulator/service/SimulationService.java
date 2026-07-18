package com.infina.price_simulator.service;

import com.infina.price_simulator.engine.SimulationEngine;
import com.infina.price_simulator.engine.SimulationMode;
import com.infina.price_simulator.engine.TaskProducer;
import com.infina.price_simulator.exceptions.SimulationAlreadyRunningException;
import com.infina.price_simulator.exceptions.SimulationNotFoundException;
import com.infina.price_simulator.metrics.ExpectedCalculator;
import com.infina.price_simulator.model.CoinCatalog;
import com.infina.price_simulator.model.CoinComparison;
import com.infina.price_simulator.model.CoinDefinition;
import com.infina.price_simulator.model.CoinRunSnapshot;
import com.infina.price_simulator.model.ExpectedValues;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.model.SimulationRunResult;
import com.infina.price_simulator.model.SimulationStats;
import com.infina.price_simulator.state.CoinState;
import com.infina.price_simulator.state.UnsafeCoinState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simülasyon akışını yönetir: görev üretimi, expected hesabı,
 * unsafe ve safe çalıştırmalar, invariant kontrolü ve sonuç yayımlama.
 * Aynı anda yalnızca bir simülasyonun çalışmasına izin verir.
 */
@Service
public class SimulationService {

    private final SimulationEngine engine;
    private final TaskProducer taskProducer;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<SimulationStats> lastStats = new AtomicReference<>();
    private final AtomicReference<List<CoinRunSnapshot>> lastSafeCoins = new AtomicReference<>();

    public SimulationService(SimulationEngine engine, TaskProducer taskProducer) {
        this.engine = engine;
        this.taskProducer = taskProducer;
    }

    public SimulationStats runSimulation(int updates, int workers, long seed) {
        if (!running.compareAndSet(false, true)) {
            throw new SimulationAlreadyRunningException(
                    "A simulation is already running."
            );
        }

        try {
            // Görev listesi yalnızca BİR kez üretilir; üç akış da aynı listeyi kullanır
            List<PriceUpdateTask> tasks = taskProducer.generate(updates, seed);

            Map<String, ExpectedValues> expected =
                    ExpectedCalculator.calculate(createCoinStates(), tasks);

            SimulationRunResult unsafeResult =
                    engine.run(tasks, workers, SimulationMode.UNSAFE);

            SimulationRunResult safeResult =
                    engine.run(tasks, workers, SimulationMode.SAFE);

            boolean invariantPassed =
                    checkSafeInvariant(safeResult, expected, tasks.size());

            SimulationStats stats = buildStats(
                    updates, workers, seed,
                    expected, unsafeResult, safeResult, invariantPassed
            );

            lastStats.set(stats);
            lastSafeCoins.set(safeResult.coins());

            return stats;
        } finally {
            running.set(false);
        }
    }

    public SimulationStats getStats() {
        SimulationStats stats = lastStats.get();

        if (stats == null) {
            throw new SimulationNotFoundException(
                    "No completed simulation result found."
            );
        }

        return stats;
    }

    public List<CoinRunSnapshot> getCoins() {
        List<CoinRunSnapshot> coins = lastSafeCoins.get();

        if (coins == null) {
            throw new SimulationNotFoundException(
                    "No completed simulation result found."
            );
        }

        return coins;
    }

    /*
     * ExpectedCalculator, CoinState listesi beklediği için başlangıç
     * fiyatlarını taşıyan kullan-at state nesneleri oluşturulur.
     */
    private List<CoinState> createCoinStates() {
        List<CoinState> states = new ArrayList<>(CoinCatalog.DEFINITIONS.size());

        for (CoinDefinition definition : CoinCatalog.DEFINITIONS) {
            states.add(new UnsafeCoinState(definition.id(), definition.initialPrice()));
        }

        return states;
    }

    /*
     * InvariantChecker CoinState beklediği ve engine snapshot döndürdüğü için
     * safe invariant kontrolü snapshot'lar üzerinden burada yapılır.
     */
    private boolean checkSafeInvariant(
            SimulationRunResult safeResult,
            Map<String, ExpectedValues> expected,
            int submittedUpdates
    ) {
        if (safeResult.processedUpdates() != submittedUpdates) {
            return false;
        }

        for (CoinRunSnapshot coin : safeResult.coins()) {
            ExpectedValues values = expected.get(coin.id());

            if (values == null) {
                return false;
            }

            if (values.expectedPrice() != coin.currentPrice()) {
                return false;
            }

            if (values.expectedUpdateCount() != coin.updateCount()) {
                return false;
            }
        }

        return true;
    }

    private SimulationStats buildStats(
            int updates,
            int workers,
            long seed,
            Map<String, ExpectedValues> expected,
            SimulationRunResult unsafeResult,
            SimulationRunResult safeResult,
            boolean invariantPassed
    ) {
        List<CoinComparison> comparisons = new ArrayList<>();

        for (CoinRunSnapshot safeCoin : safeResult.coins()) {
            String id = safeCoin.id();
            ExpectedValues expectedValues = expected.get(id);
            CoinRunSnapshot unsafeCoin = findCoin(unsafeResult, id);

            comparisons.add(new CoinComparison(
                    id,
                    safeCoin.initialPrice(),
                    expectedValues.expectedPrice(),
                    unsafeCoin.currentPrice(),
                    safeCoin.currentPrice(),
                    expectedValues.expectedUpdateCount(),
                    unsafeCoin.updateCount(),
                    safeCoin.updateCount()
            ));
        }

        return new SimulationStats(
                updates,
                workers,
                seed,
                unsafeResult.elapsedMillis(),
                safeResult.elapsedMillis(),
                unsafeResult.throughputPerSecond(),
                safeResult.throughputPerSecond(),
                unsafeResult.processedUpdates(),
                safeResult.processedUpdates(),
                comparisons,
                invariantPassed
        );
    }

    private CoinRunSnapshot findCoin(SimulationRunResult result, String id) {
        return result.coins().stream()
                .filter(coin -> coin.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Coin not found in result: " + id
                ));
    }
}