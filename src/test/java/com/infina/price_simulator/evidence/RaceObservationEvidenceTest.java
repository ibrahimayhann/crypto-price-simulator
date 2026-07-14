package com.infina.price_simulator.evidence;

import com.infina.price_simulator.engine.SimulationEngine;
import com.infina.price_simulator.engine.SimulationMode;
import com.infina.price_simulator.engine.TaskProducer;
import com.infina.price_simulator.model.CoinRunSnapshot;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.model.SimulationRunResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Race condition gözlemini belgeleyen kanıt testi.
 *
 * <p>UNSAFE modda birden fazla worker ile çalıştırıldığında counter ve coin
 * state güncellemelerinde race condition nedeniyle kayıpların oluştuğunu gösterir.
 * SAFE modun ise her koşulda doğru sonuç ürettiğini doğrular.</p>
 */
@EnabledIfSystemProperty(
        named = "raceObservation",
        matches = "true"
)
class RaceObservationEvidenceTest {

    private static final int UPDATE_COUNT = 50_000;
    private static final long SEED = 42L;
    private static final int WORKER_COUNT = 4;
    private static final int RUN_COUNT = 5;

    private final TaskProducer taskProducer = new TaskProducer();
    private final SimulationEngine simulationEngine = new SimulationEngine();

    @Test
    void observeRaceConditionAcrossMultipleRuns() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(UPDATE_COUNT, SEED);

        System.out.println();
        System.out.printf(
                "Race Observation — seed=%d, updates=%,d, workers=%d, runs=%d%n%n",
                SEED, UPDATE_COUNT, WORKER_COUNT, RUN_COUNT
        );

        System.out.println(
                "| Run | Unsafe processed | Safe processed | "
                        + "Unsafe counter sapma | Safe invariant |"
        );
        System.out.println("|---:|---:|---:|---|---|");

        int unsafeCounterDeviation = 0;
        int unsafeCoinDeviation = 0;
        int safeInvariantSuccess = 0;

        for (int run = 1; run <= RUN_COUNT; run++) {
            SimulationRunResult unsafeResult =
                    simulationEngine.run(tasks, WORKER_COUNT, SimulationMode.UNSAFE);

            SimulationRunResult safeResult =
                    simulationEngine.run(tasks, WORKER_COUNT, SimulationMode.SAFE);

            long unsafeProcessed = unsafeResult.processedUpdates();
            long safeProcessed = safeResult.processedUpdates();

            boolean counterOk = unsafeProcessed == UPDATE_COUNT;
            boolean safeInvariantOk = checkSafeInvariant(tasks, safeResult.coins());

            if (!counterOk) {
                unsafeCounterDeviation++;
            }

            if (checkUnsafeCoinDeviation(tasks, unsafeResult.coins())) {
                unsafeCoinDeviation++;
            }

            if (safeInvariantOk) {
                safeInvariantSuccess++;
            }

            System.out.printf(
                    "| %d | %,d | %,d | %s | %s |%n",
                    run,
                    unsafeProcessed,
                    safeProcessed,
                    counterOk ? "Kayıp yok" : "KAYIP=" + (UPDATE_COUNT - unsafeProcessed),
                    safeInvariantOk ? "Başarılı" : "BAŞARISIZ"
            );
        }

        System.out.println();
        System.out.printf("Unsafe counter sapma görülen run: %d/%d%n", unsafeCounterDeviation, RUN_COUNT);
        System.out.printf("Unsafe coin sapma görülen run: %d/%d%n", unsafeCoinDeviation, RUN_COUNT);
        System.out.printf("Safe invariant başarılı run: %d/%d%n", safeInvariantSuccess, RUN_COUNT);
        System.out.println();
    }

    private boolean checkSafeInvariant(
            List<PriceUpdateTask> tasks,
            List<CoinRunSnapshot> snapshots
    ) {
        Map<String, Long> expectedPrices = new HashMap<>();
        expectedPrices.put("BTC", 60_000L);
        expectedPrices.put("ETH", 3_000L);
        expectedPrices.put("SOL", 150L);

        Map<String, Long> expectedCounts = new HashMap<>();
        expectedCounts.put("BTC", 0L);
        expectedCounts.put("ETH", 0L);
        expectedCounts.put("SOL", 0L);

        for (PriceUpdateTask task : tasks) {
            expectedPrices.computeIfPresent(task.coinId(),
                    (id, price) -> price + task.delta());
            expectedCounts.computeIfPresent(task.coinId(),
                    (id, count) -> count + 1L);
        }

        for (CoinRunSnapshot snapshot : snapshots) {
            if (!expectedPrices.get(snapshot.id()).equals(snapshot.currentPrice())) {
                return false;
            }
            if (!expectedCounts.get(snapshot.id()).equals(snapshot.updateCount())) {
                return false;
            }
        }
        return true;
    }

    private boolean checkUnsafeCoinDeviation(
            List<PriceUpdateTask> tasks,
            List<CoinRunSnapshot> snapshots
    ) {
        Map<String, Long> expectedPrices = new HashMap<>();
        expectedPrices.put("BTC", 60_000L);
        expectedPrices.put("ETH", 3_000L);
        expectedPrices.put("SOL", 150L);

        Map<String, Long> expectedCounts = new HashMap<>();
        expectedCounts.put("BTC", 0L);
        expectedCounts.put("ETH", 0L);
        expectedCounts.put("SOL", 0L);

        for (PriceUpdateTask task : tasks) {
            expectedPrices.computeIfPresent(task.coinId(),
                    (id, price) -> price + task.delta());
            expectedCounts.computeIfPresent(task.coinId(),
                    (id, count) -> count + 1L);
        }

        for (CoinRunSnapshot snapshot : snapshots) {
            if (!expectedPrices.get(snapshot.id()).equals(snapshot.currentPrice())
                    || !expectedCounts.get(snapshot.id()).equals(snapshot.updateCount())) {
                return true; // sapma var
            }
        }
        return false;
    }
}
