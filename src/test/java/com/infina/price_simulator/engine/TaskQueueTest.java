package com.infina.price_simulator.engine;

import com.infina.price_simulator.model.PriceUpdateTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskQueueTest {

    private ExecutorService executor;

    @AfterEach
    void shutDownExecutor() throws InterruptedException {
        if (executor == null) {
            return;
        }

        executor.shutdownNow();

        assertTrue(
                executor.awaitTermination(1, TimeUnit.SECONDS),
                "Test executor did not terminate in time."
        );
    }

    @Test
    void constructorRejectsZeroCapacity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TaskQueue(0)
        );
    }

    @Test
    void constructorRejectsNegativeCapacity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TaskQueue(-1)
        );
    }

    @Test
    void putAndTakeReturnTheSameTask() throws InterruptedException {
        TaskQueue taskQueue = new TaskQueue(10);
        PriceUpdateTask task =
                new PriceUpdateTask(1L, "BTC", 25L);

        taskQueue.put(task);

        assertEquals(task, taskQueue.take());
    }

    @Test
    void queuePreservesFifoOrder() throws InterruptedException {
        TaskQueue taskQueue = new TaskQueue(3);

        PriceUpdateTask first =
                new PriceUpdateTask(1L, "BTC", 10L);

        PriceUpdateTask second =
                new PriceUpdateTask(2L, "ETH", -5L);

        PriceUpdateTask third =
                new PriceUpdateTask(3L, "SOL", 2L);

        taskQueue.put(first);
        taskQueue.put(second);
        taskQueue.put(third);

        assertEquals(first, taskQueue.take());
        assertEquals(second, taskQueue.take());
        assertEquals(third, taskQueue.take());
    }

    @Test
    void putRejectsNullTask() {
        TaskQueue taskQueue = new TaskQueue(1);

        assertThrows(
                NullPointerException.class,
                () -> taskQueue.put(null)
        );
    }

    @Test
    void putPoisonPillsAddsOnePillPerWorker()
            throws InterruptedException {

        TaskQueue taskQueue = new TaskQueue(3);

        taskQueue.putPoisonPills(3);

        assertEquals(3, taskQueue.size());

        assertTrue(TaskQueue.isPoisonPill(taskQueue.take()));
        assertTrue(TaskQueue.isPoisonPill(taskQueue.take()));
        assertTrue(TaskQueue.isPoisonPill(taskQueue.take()));
    }

    @Test
    void putPoisonPillsRejectsNonPositiveWorkerCount() {
        TaskQueue taskQueue = new TaskQueue(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> taskQueue.putPoisonPills(0)
        );
    }

    @Test
    void regularTaskIsNotIdentifiedAsPoisonPill() {
        PriceUpdateTask regularTask =
                new PriceUpdateTask(1L, "BTC", 10L);

        assertFalse(TaskQueue.isPoisonPill(regularTask));
    }

    @Test
    void takeWaitsUntilTaskBecomesAvailable()
            throws Exception {

        TaskQueue taskQueue = new TaskQueue(1);
        PriceUpdateTask expectedTask =
                new PriceUpdateTask(1L, "BTC", 10L);

        executor = Executors.newSingleThreadExecutor();

        Future<PriceUpdateTask> waitingConsumer =
                executor.submit(taskQueue::take);

        /*
         * Zaman aşımı oluşması, kuyruk boşken take() metodunun geri dönmediğini
         * kanıtlar. Böylece senkronizasyon amacıyla Thread.sleep() kullanmaya
         * gerek kalmaz.
         */

        assertThrows(
                TimeoutException.class,
                () -> waitingConsumer.get(
                        100,
                        TimeUnit.MILLISECONDS
                )
        );

        taskQueue.put(expectedTask);

        assertEquals(
                expectedTask,
                waitingConsumer.get(1, TimeUnit.SECONDS)
        );
    }

    @Test
    void putWaitsWhenBoundedQueueIsFull()
            throws Exception {

        TaskQueue taskQueue = new TaskQueue(1);

        PriceUpdateTask firstTask =
                new PriceUpdateTask(1L, "BTC", 10L);

        PriceUpdateTask secondTask =
                new PriceUpdateTask(2L, "ETH", 20L);

        taskQueue.put(firstTask);

        executor = Executors.newSingleThreadExecutor();
        CountDownLatch producerStarted = new CountDownLatch(1);

        Future<?> blockedProducer = executor.submit(() -> {
            producerStarted.countDown();
            taskQueue.put(secondTask);
            return null;
        });

        assertTrue(
                producerStarted.await(1, TimeUnit.SECONDS)
        );

        /*
        * Tek kuyruk yuvası dolu olduğu sürece ikinci görev kuyruğa eklenemez.
        * Bu durum, sınırlı kapasiteli kuyruklarda oluşan geri baskı
        * mekanizmasını gösterir.
        */
        assertThrows(
                TimeoutException.class,
                () -> blockedProducer.get(
                        100,
                        TimeUnit.MILLISECONDS
                )
        );

        assertEquals(firstTask, taskQueue.take());

        blockedProducer.get(1, TimeUnit.SECONDS);

        assertEquals(secondTask, taskQueue.take());
    }

    @Test
    void capacityAndRemainingCapacityAreReportedCorrectly()
            throws InterruptedException {

        TaskQueue taskQueue = new TaskQueue(2);

        assertEquals(2, taskQueue.capacity());
        assertEquals(2, taskQueue.remainingCapacity());

        taskQueue.put(
                new PriceUpdateTask(1L, "BTC", 10L)
        );

        assertEquals(1, taskQueue.remainingCapacity());
    }
}