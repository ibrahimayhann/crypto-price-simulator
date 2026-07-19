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

import static org.junit.jupiter.api.Assertions.assertEquals;

@EnabledIfSystemProperty(
        named = "benchmark",
        matches = "true"
)
class SimulationBenchmarkTest {

    private static final int UPDATE_COUNT = 50_000;
    private static final long SEED = 42L;

    private final TaskProducer taskProducer =
            new TaskProducer();

    private final SimulationEngine simulationEngine =
            new SimulationEngine();

    @Test
    void compareOneTwoFourAndEightWorkers() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(
                        UPDATE_COUNT,
                        SEED
                );

        int[] workerCounts = {1, 2, 4, 8};

        System.out.println();
        System.out.println(
                "| Updates | Workers | Unsafe ms | Safe ms "
                        + "| Safe throughput | Safe invariant |"
        );

        System.out.println(
                "|---:|---:|---:|---:|---:|---|"
        );

        for (int workerCount : workerCounts) {
            SimulationRunResult unsafeResult =
                    simulationEngine.run(
                            tasks,
                            workerCount,
                            SimulationMode.UNSAFE
                    );

            SimulationRunResult safeResult =
                    simulationEngine.run(
                            tasks,
                            workerCount,
                            SimulationMode.SAFE
                    );

            assertSafeInvariant(tasks, safeResult.coins());

            System.out.printf(
                    "| %,d | %d | %d | %d | %,.0f | Successful |%n",
                    UPDATE_COUNT,
                    workerCount,
                    unsafeResult.elapsedMillis(),
                    safeResult.elapsedMillis(),
                    safeResult.throughputPerSecond()
            );
        }

        System.out.println();
    }

    @Test
    void compareVirtualThreadsBonus() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(
                        UPDATE_COUNT,
                        SEED
                );

        int workerCount = 4; 

        System.out.println();
        System.out.println("--- BONUS A: VIRTUAL THREADS (CPU-Bound) ---");
        System.out.println("| Mode | Elapsed ms | Throughput/s |");
        System.out.println("|---|---:|---:|");

        SimulationRunResult platformResult = simulationEngine.run(tasks, workerCount, SimulationMode.SAFE, false, false);
        System.out.printf("| Platform (Fixed-4) | %d | %,.0f |%n", platformResult.elapsedMillis(), platformResult.throughputPerSecond());

        SimulationRunResult virtualResult = simulationEngine.run(tasks, workerCount, SimulationMode.SAFE, true, false);
        System.out.printf("| Virtual Threads | %d | %,.0f |%n", virtualResult.elapsedMillis(), virtualResult.throughputPerSecond());
        System.out.println();

        System.out.println("--- BONUS A: VIRTUAL THREADS (I/O-Bound, 1ms sleep per task) ---");
        System.out.println("| Mode | Elapsed ms | Throughput/s |");
        System.out.println("|---|---:|---:|");

        
        int ioUpdateCount = 1_000;
        List<PriceUpdateTask> ioTasks = taskProducer.generate(ioUpdateCount, SEED);

        SimulationRunResult platformIoResult = simulationEngine.run(ioTasks, workerCount, SimulationMode.SAFE, false, true);
        System.out.printf("| Platform (Fixed-4) | %d | %,.0f |%n", platformIoResult.elapsedMillis(), platformIoResult.throughputPerSecond());

        SimulationRunResult virtualIoResult = simulationEngine.run(ioTasks, workerCount, SimulationMode.SAFE, true, true);
        System.out.printf("| Virtual Threads | %d | %,.0f |%n", virtualIoResult.elapsedMillis(), virtualIoResult.throughputPerSecond());
        System.out.println();
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