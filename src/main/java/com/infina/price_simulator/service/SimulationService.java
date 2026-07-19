package com.infina.price_simulator.service;

import com.infina.price_simulator.api.exception.SimulationAlreadyRunningException;
import com.infina.price_simulator.api.exception.SimulationNotFoundException;
import com.infina.price_simulator.engine.SimulationEngine;
import com.infina.price_simulator.engine.SimulationMode;
import com.infina.price_simulator.engine.TaskProducer;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class SimulationService {

    private final SimulationEngine engine;
    private final TaskProducer taskProducer;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<SimulationStats> lastStats = new AtomicReference<>();
    private final AtomicReference<List<CoinRunSnapshot>> lastSafeCoins = new AtomicReference<>();

    public SimulationStats runSimulation(int updates, int workers, long seed) {
        if (!running.compareAndSet(false, true)) {
            throw new SimulationAlreadyRunningException(
                    "A simulation is already running."
            );
        }

        try {
            SimulationExecution execution =
                    executeSimulation(updates, workers, seed);

            SimulationStats stats =
                    buildStats(updates, workers, seed, execution);

            publishResult(stats, execution.safeResult());

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

    private SimulationExecution executeSimulation(
            int updates,
            int workers,
            long seed
    ) {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(updates, seed);

        Map<String, ExpectedValues> expectedValues =
                calculateExpectedValues(tasks);

        SimulationRunResult unsafeResult =
                runEngine(tasks, workers, SimulationMode.UNSAFE);

        SimulationRunResult safeResult =
                runEngine(tasks, workers, SimulationMode.SAFE);

        boolean safeInvariantPassed =
                isSafeInvariantPassed(safeResult, expectedValues, tasks.size());

        return new SimulationExecution(
                expectedValues,
                unsafeResult,
                safeResult,
                safeInvariantPassed
        );
    }

    private SimulationStats buildStats(
            int updates,
            int workers,
            long seed,
            SimulationExecution execution
    ) {
        SimulationRunResult unsafeResult = execution.unsafeResult();
        SimulationRunResult safeResult = execution.safeResult();

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
                buildCoinComparisons(execution),
                execution.safeInvariantPassed()
        );
    }

    private Map<String, ExpectedValues> calculateExpectedValues(
            List<PriceUpdateTask> tasks
    ) {
        return ExpectedCalculator.calculate(createInitialCoinStates(), tasks);
    }

    private List<CoinState> createInitialCoinStates() {
        List<CoinState> states =
                new ArrayList<>(CoinCatalog.DEFINITIONS.size());

        for (CoinDefinition definition : CoinCatalog.DEFINITIONS) {
            states.add(new UnsafeCoinState(
                    definition.id(),
                    definition.initialPrice()
            ));
        }

        return states;
    }

    private SimulationRunResult runEngine(
            List<PriceUpdateTask> tasks,
            int workers,
            SimulationMode mode
    ) {
        return engine.run(tasks, workers, mode);
    }

    private boolean isSafeInvariantPassed(
            SimulationRunResult safeResult,
            Map<String, ExpectedValues> expectedValues,
            int submittedUpdates
    ) {
        return safeResult.processedUpdates() == submittedUpdates
                && matchesExpectedValues(safeResult.coins(), expectedValues);
    }

    private boolean matchesExpectedValues(
            List<CoinRunSnapshot> coins,
            Map<String, ExpectedValues> expectedValues
    ) {
        for (CoinRunSnapshot coin : coins) {
            ExpectedValues expected = expectedValues.get(coin.id());

            if (expected == null) {
                return false;
            }

            if (expected.expectedPrice() != coin.currentPrice()) {
                return false;
            }

            if (expected.expectedUpdateCount() != coin.updateCount()) {
                return false;
            }
        }

        return true;
    }

    private List<CoinComparison> buildCoinComparisons(
            SimulationExecution execution
    ) {
        List<CoinRunSnapshot> safeCoins =
                execution.safeResult().coins();

        Map<String, CoinRunSnapshot> unsafeCoinsById =
                indexCoinsById(execution.unsafeResult().coins());

        List<CoinComparison> comparisons =
                new ArrayList<>(safeCoins.size());

        for (CoinRunSnapshot safeCoin : safeCoins) {
            comparisons.add(toCoinComparison(
                    safeCoin,
                    findCoin(unsafeCoinsById, safeCoin.id()),
                    findExpectedValues(execution.expectedValues(), safeCoin.id())
            ));
        }

        return comparisons;
    }

    private CoinComparison toCoinComparison(
            CoinRunSnapshot safeCoin,
            CoinRunSnapshot unsafeCoin,
            ExpectedValues expectedValues
    ) {
        return new CoinComparison(
                safeCoin.id(),
                safeCoin.initialPrice(),
                expectedValues.expectedPrice(),
                unsafeCoin.currentPrice(),
                safeCoin.currentPrice(),
                expectedValues.expectedUpdateCount(),
                unsafeCoin.updateCount(),
                safeCoin.updateCount()
        );
    }

    private Map<String, CoinRunSnapshot> indexCoinsById(
            List<CoinRunSnapshot> coins
    ) {
        Map<String, CoinRunSnapshot> indexedCoins =
                new LinkedHashMap<>();

        for (CoinRunSnapshot coin : coins) {
            indexedCoins.put(coin.id(), coin);
        }

        return indexedCoins;
    }

    private CoinRunSnapshot findCoin(
            Map<String, CoinRunSnapshot> coinsById,
            String id
    ) {
        CoinRunSnapshot coin = coinsById.get(id);

        if (coin == null) {
            throw new IllegalStateException(
                    "Coin not found in result: " + id
            );
        }

        return coin;
    }

    private ExpectedValues findExpectedValues(
            Map<String, ExpectedValues> expectedValuesById,
            String id
    ) {
        ExpectedValues expectedValues = expectedValuesById.get(id);

        if (expectedValues == null) {
            throw new IllegalStateException(
                    "Expected values not found for coin: " + id
            );
        }

        return expectedValues;
    }

    private void publishResult(
            SimulationStats stats,
            SimulationRunResult safeResult
    ) {
        lastStats.set(stats);
        lastSafeCoins.set(safeResult.coins());
    }

    private record SimulationExecution(
            Map<String, ExpectedValues> expectedValues,
            SimulationRunResult unsafeResult,
            SimulationRunResult safeResult,
            boolean safeInvariantPassed
    ) {
    }
}
