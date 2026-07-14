package com.infina.price_simulator.engine;

import com.infina.price_simulator.model.CoinRunSnapshot;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.model.SimulationRunResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulationEngineTest {

    private final TaskProducer taskProducer =
            new TaskProducer();

    private final SimulationEngine simulationEngine =
            new SimulationEngine();

    @Test
    void safeRunProcessesEverySubmittedTask() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(10_000, 42L);

        SimulationRunResult result =
                simulationEngine.run(
                        tasks,
                        4,
                        SimulationMode.SAFE
                );

        assertEquals(10_000, result.submittedUpdates());
        assertEquals(10_000L, result.processedUpdates());
        assertEquals(4, result.workers());
        assertEquals(3, result.coins().size());
        assertTrue(result.elapsedNanos() > 0L);
        assertTrue(result.throughputPerSecond() > 0.0);

        assertSafeInvariant(tasks, result.coins());
    }

    @Test
    void safeRunTerminatesNamedWorkerThreads() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(1_000, 42L);

        simulationEngine.run(
                tasks,
                4,
                SimulationMode.SAFE
        );

        boolean activeSafeWorkerExists =
                Thread.getAllStackTraces()
                        .keySet()
                        .stream()
                        .anyMatch(thread ->
                                thread.isAlive()
                                        && thread.getName()
                                        .startsWith("safe-worker-")
                        );

        assertFalse(
                activeSafeWorkerExists,
                "Safe worker threads remained alive after engine completion."
        );
    }

    @Test
    void unsafeRunDoesNotAssumeRaceMustAlwaysOccur() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(10_000, 42L);

        SimulationRunResult result =
                simulationEngine.run(
                        tasks,
                        8,
                        SimulationMode.UNSAFE
                );

        assertEquals(10_000, result.submittedUpdates());
        assertTrue(result.processedUpdates() >= 0L);
        assertTrue(result.processedUpdates() <= 10_000L);

        /*
         * No assertion requires unsafe output to be incorrect because race
         * conditions are timing-dependent and may not appear in every run.
         */
    }

    @Test
    void rejectsWorkerCountBelowMinimum() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(10, 42L);

        assertThrows(
                IllegalArgumentException.class,
                () -> simulationEngine.run(
                        tasks,
                        0,
                        SimulationMode.SAFE
                )
        );
    }

    @Test
    void rejectsWorkerCountAboveMaximum() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(10, 42L);

        assertThrows(
                IllegalArgumentException.class,
                () -> simulationEngine.run(
                        tasks,
                        17,
                        SimulationMode.SAFE
                )
        );
    }

    @Test
    void rejectsEmptyTaskList() {
        assertThrows(
                IllegalArgumentException.class,
                () -> simulationEngine.run(
                        List.of(),
                        4,
                        SimulationMode.SAFE
                )
        );
    }

    @Test
    void reportsWorkerFailureForUnknownCoin() {
        List<PriceUpdateTask> tasks =
                List.of(
                        new PriceUpdateTask(
                                1L,
                                "DOGE",
                                10L
                        )
                );

        assertThrows(
                SimulationEngineException.class,
                () -> simulationEngine.run(
                        tasks,
                        1,
                        SimulationMode.SAFE
                )
        );
    }

    private void assertSafeInvariant(
            List<PriceUpdateTask> tasks,
            List<CoinRunSnapshot> snapshots
    ) {
        Map<String, Long> expectedPrices =
                new HashMap<>();

        expectedPrices.put("BTC", 60_000L);
        expectedPrices.put("ETH", 3_000L);
        expectedPrices.put("SOL", 150L);

        Map<String, Long> expectedCounts =
                new HashMap<>();

        expectedCounts.put("BTC", 0L);
        expectedCounts.put("ETH", 0L);
        expectedCounts.put("SOL", 0L);

        for (PriceUpdateTask task : tasks) {
            expectedPrices.computeIfPresent(
                    task.coinId(),
                    (coinId, price) ->
                            price + task.delta()
            );

            expectedCounts.computeIfPresent(
                    task.coinId(),
                    (coinId, count) ->
                            count + 1L
            );
        }

        for (CoinRunSnapshot snapshot : snapshots) {
            assertEquals(
                    expectedPrices.get(snapshot.id()),
                    snapshot.currentPrice()
            );

            assertEquals(
                    expectedCounts.get(snapshot.id()),
                    snapshot.updateCount()
            );
        }
    }
}