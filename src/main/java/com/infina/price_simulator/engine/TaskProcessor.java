package com.infina.price_simulator.engine;

import com.infina.price_simulator.counter.Counter;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.state.CoinState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ortak görev işleme mantığı.
 * Hem sabit havuzdaki worker'lar hem de Virtual Thread'ler tarafından
 * paylaşılan stateless işlem metodudur.
 */
public final class TaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskProcessor.class);

    private TaskProcessor() {
        // Utility class
    }

    public static void process(
            PriceUpdateTask task,
            Map<String, CoinState> coinStatesById,
            Counter processedCounter,
            CountDownLatch completionLatch,
            AtomicReference<Throwable> firstFailure,
            boolean simulateIoWait
    ) {
        try {
            if (simulateIoWait && !doIoWait()) {
                return;
            }

            CoinState coinState = coinStatesById.get(task.coinId());
            if (coinState == null) {
                throw new IllegalArgumentException("Unsupported coin id: " + task.coinId());
            }

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

    private static boolean doIoWait() {
        try {
            Thread.sleep(1);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
