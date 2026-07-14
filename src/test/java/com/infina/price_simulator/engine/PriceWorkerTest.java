package com.infina.price_simulator.engine;

import com.infina.price_simulator.counter.SafeCounter;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.state.CoinState;
import com.infina.price_simulator.state.SafeCoinState;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceWorkerTest {

    @Test
    void processesTaskAndStopsAfterPoisonPill()
            throws Exception {

        TaskQueue taskQueue = new TaskQueue(2);
        SafeCoinState btc = new SafeCoinState("BTC", 60_000L);
        SafeCounter counter = new SafeCounter();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure =
                new AtomicReference<>();

        Map<String, CoinState> coins = Map.of("BTC", btc);

        taskQueue.put(
                new PriceUpdateTask(1L, "BTC", 120L)
        );

        taskQueue.putPoisonPills(1);

        PriceWorker worker = new PriceWorker(
                taskQueue,
                coins,
                counter,
                latch,
                failure
        );

        Thread workerThread =
                new Thread(worker, "safe-worker-test");

        workerThread.start();

        assertTrue(
                latch.await(1, TimeUnit.SECONDS),
                "Worker did not process the task in time."
        );

        workerThread.join(1_000);

        assertFalse(workerThread.isAlive());
        assertEquals(60_120L, btc.getCurrentPrice());
        assertEquals(1L, btc.getUpdateCount());
        assertEquals(120L, btc.getLastDelta());
        assertEquals("safe-worker-test", btc.getLastUpdatedBy());
        assertEquals(1L, counter.get());
        assertNull(failure.get());
    }

    @Test
    void recordsUnsupportedCoinFailureWithoutHanging()
            throws Exception {

        TaskQueue taskQueue = new TaskQueue(2);
        SafeCounter counter = new SafeCounter();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure =
                new AtomicReference<>();

        taskQueue.put(
                new PriceUpdateTask(1L, "DOGE", 10L)
        );

        taskQueue.putPoisonPills(1);

        PriceWorker worker = new PriceWorker(
                taskQueue,
                Map.of(),
                counter,
                latch,
                failure
        );

        Thread workerThread =
                new Thread(worker, "safe-worker-test");

        workerThread.start();

        assertTrue(latch.await(1, TimeUnit.SECONDS));

        workerThread.join(1_000);

        assertFalse(workerThread.isAlive());
        assertEquals(0L, counter.get());

        assertInstanceOf(
                IllegalArgumentException.class,
                failure.get()
        );
    }
}