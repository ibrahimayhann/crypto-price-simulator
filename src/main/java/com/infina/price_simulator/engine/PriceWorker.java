package com.infina.price_simulator.engine;

import com.infina.price_simulator.counter.Counter;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.state.CoinState;


import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Paylaşılan bloke edici kuyruktaki fiyat güncelleme görevlerini tüketir.
 *
 * <p>Her işçi iş parçacığı sürekli olarak bir görev gelmesini bekler, ilgili
 * coin'in fiyatını günceller, işlenen görev sayacını artırır ve görevin
 * tamamlandığını completion latch'e bildirir.</p>
 */

public final class PriceWorker implements Runnable {


    private final TaskQueue taskQueue;
    private final Map<String, CoinState> coinStatesById;
    private final Counter processedCounter;
    private final CountDownLatch completionLatch;
    private final AtomicReference<Throwable> firstFailure;
    private final boolean simulateIoWait;

    public PriceWorker(
            TaskQueue taskQueue,
            Map<String, CoinState> coinStatesById,
            Counter processedCounter,
            CountDownLatch completionLatch,
            AtomicReference<Throwable> firstFailure
    ) {
        this(taskQueue, coinStatesById, processedCounter, completionLatch, firstFailure, false);
    }

    public PriceWorker(
            TaskQueue taskQueue,
            Map<String, CoinState> coinStatesById,
            Counter processedCounter,
            CountDownLatch completionLatch,
            AtomicReference<Throwable> firstFailure,
            boolean simulateIoWait
    ) {
        this.taskQueue = Objects.requireNonNull(
                taskQueue,
                "Task queue must not be null."
        );

        this.coinStatesById = Objects.requireNonNull(
                coinStatesById,
                "Coin states must not be null."
        );

        this.processedCounter = Objects.requireNonNull(
                processedCounter,
                "Processed counter must not be null."
        );

        this.completionLatch = Objects.requireNonNull(
                completionLatch,
                "Completion latch must not be null."
        );

        this.firstFailure = Objects.requireNonNull(
                firstFailure,
                "Failure holder must not be null."
        );

        this.simulateIoWait = simulateIoWait;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            PriceUpdateTask task;

            try {
                task = taskQueue.take();
            } catch (InterruptedException exception) {

                /*
                 * Bu işçi iş parçacığının kesintiye uğradığının çağıran kod ve executor
                 * kapatma mantığı tarafından algılanabilmesi için kesinti bayrağını
                 * yeniden ayarla.
                 */
                Thread.currentThread().interrupt();
                return;
            }

            if (TaskQueue.isPoisonPill(task)) {
                return;
            }

            processTaskSafely(task);
        }
    }

    private void processTaskSafely(PriceUpdateTask task) {
        TaskProcessor.process(
                task,
                coinStatesById,
                processedCounter,
                completionLatch,
                firstFailure,
                simulateIoWait
        );
    }
}