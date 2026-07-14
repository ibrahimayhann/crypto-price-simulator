package com.infina.price_simulator.evidence;

import com.infina.price_simulator.counter.SafeCounter;
import com.infina.price_simulator.engine.PriceWorker;
import com.infina.price_simulator.engine.TaskQueue;
import com.infina.price_simulator.state.CoinState;
import com.infina.price_simulator.state.SafeCoinState;
import com.infina.price_simulator.util.WorkerThreadFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(
        named = "threadDumpDemo",
        matches = "true"
)
class ThreadDumpEvidenceTest {

    private static final int WORKER_COUNT = 4;
    private static final int CAPTURE_WINDOW_SECONDS = 30;

    @Test
    void exposeNamedWorkersForThreadDump()
            throws Exception {

        TaskQueue taskQueue = new TaskQueue(16);

        Map<String, CoinState> coinStates = Map.of(
                "BTC", new SafeCoinState("BTC", 60_000L),
                "ETH", new SafeCoinState("ETH", 3_000L),
                "SOL", new SafeCoinState("SOL", 150L)
        );

        CountDownLatch completionLatch =
                new CountDownLatch(0);

        AtomicReference<Throwable> firstFailure =
                new AtomicReference<>();

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        WORKER_COUNT,
                        new WorkerThreadFactory(
                                "safe-worker-"
                        )
                );

        for (int index = 0; index < WORKER_COUNT; index++) {
            executor.submit(
                    new PriceWorker(
                            taskQueue,
                            coinStates,
                            new SafeCounter(),
                            completionLatch,
                            firstFailure
                    )
            );
        }

        long processId =
                ProcessHandle.current().pid();

        System.out.println();
        System.out.println(
                "Thread dump evidence JVM PID: "
                        + processId
        );

        System.out.println(
                "Workers will wait on BlockingQueue.take() for "
                        + CAPTURE_WINDOW_SECONDS
                        + " seconds."
        );

        System.out.println(
                "Run in another terminal:"
        );

        System.out.println(
                "jcmd "
                        + processId
                        + " Thread.print > docs\\evidence\\thread-dump.txt"
        );

        /*
         * This sleep belongs only to the manual evidence harness. It is not
         * used as a synchronization mechanism in production or unit tests.
         */
        TimeUnit.SECONDS.sleep(
                CAPTURE_WINDOW_SECONDS
        );

        taskQueue.putPoisonPills(WORKER_COUNT);

        executor.shutdown();

        assertTrue(
                executor.awaitTermination(
                        5,
                        TimeUnit.SECONDS
                ),
                "Evidence workers did not terminate."
        );
    }
}