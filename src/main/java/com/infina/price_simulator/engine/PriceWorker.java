package com.infina.price_simulator.engine;

import com.infina.price_simulator.counter.Counter;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.state.CoinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOGGER =
            LoggerFactory.getLogger(PriceWorker.class);

    private final TaskQueue taskQueue;
    private final Map<String, CoinState> coinStatesById;
    private final Counter processedCounter;
    private final CountDownLatch completionLatch;
    private final AtomicReference<Throwable> firstFailure;

    public PriceWorker(
            TaskQueue taskQueue,
            Map<String, CoinState> coinStatesById,
            Counter processedCounter,
            CountDownLatch completionLatch,
            AtomicReference<Throwable> firstFailure
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
        try {
            CoinState coinState = findCoinState(task.coinId());

            coinState.applyDelta(task.delta());
            processedCounter.increment();


            /*
             * Her görev için yapılan loglama yalnızca DEBUG seviyesindedir.
             * Aksi hâlde konsol çıktısı benchmark sonuçlarını büyük ölçüde etkiler.
             */

            LOGGER.debug(
                    "[{}] {} delta={} sequence={}",
                    Thread.currentThread().getName(),
                    task.coinId(),
                    task.delta(),
                    task.sequence()
            );
        } catch (RuntimeException exception) {

            /*
             * Yalnızca ilk hatayı kaydet. İşçi iş parçacığı görevleri tüketmeye devam eder;
             * böylece latch sayacı kalıcı olarak sıfırın üzerinde kalmaz.
             */

            firstFailure.compareAndSet(null, exception);

            LOGGER.error(
                    "Worker {} failed while processing task sequence={}",
                    Thread.currentThread().getName(),
                    task.sequence(),
                    exception
            );
        } finally {

            /*
             * Başarısız olanlar da dâhil olmak üzere her gerçek görev, latch sayacını
             * tam olarak bir kez azaltmalıdır. Sonlandırma işaretleri (poison pill)
             * bu metoda hiçbir zaman ulaşmaz.
             */
            completionLatch.countDown();
        }
    }

    private CoinState findCoinState(String coinId) {
        CoinState coinState = coinStatesById.get(coinId);

        if (coinState == null) {
            throw new IllegalArgumentException(
                    "Unsupported coin id: " + coinId
            );
        }

        return coinState;
    }
}