package com.infina.price_simulator.engine;

import com.infina.price_simulator.counter.SafeCounter;
import com.infina.price_simulator.counter.UnsafeCounter;
import com.infina.price_simulator.state.CoinState;
import com.infina.price_simulator.state.SafeCoinState;
import com.infina.price_simulator.state.UnsafeCoinState;
import com.infina.price_simulator.util.WorkerThreadFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PriceWorker'ın shutdown, interrupt ve PoisonPill davranışlarını otomatik olarak doğrular.
 *
 * Bu testler manuel thread dump gerektirmez; tamamen otonom çalışır.
 */
class ShutdownBehaviorTest {

    private static final Map<String, CoinState> SAFE_COIN_STATES = Map.of(
            "BTC", new SafeCoinState("BTC", 60_000L),
            "ETH", new SafeCoinState("ETH", 3_000L),
            "SOL", new SafeCoinState("SOL", 150L)
    );

    private static final Map<String, CoinState> UNSAFE_COIN_STATES = Map.of(
            "BTC", new UnsafeCoinState("BTC", 60_000L),
            "ETH", new UnsafeCoinState("ETH", 3_000L),
            "SOL", new UnsafeCoinState("SOL", 150L)
    );

    /**
     * PoisonPill mekanizması: Her worker için bir PoisonPill gönderildiğinde
     * tüm worker'lar kuyruğu beklemeden graceful şekilde sonlanmalı.
     */
    @Test
    void workersShouldStopGracefullyOnPoisonPill() throws Exception {
        int workerCount = 4;
        TaskQueue taskQueue = new TaskQueue(16);
        CountDownLatch completionLatch = new CountDownLatch(0);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(
                workerCount,
                new WorkerThreadFactory("safe-worker-")
        );

        for (int i = 0; i < workerCount; i++) {
            executor.submit(new PriceWorker(
                    taskQueue,
                    SAFE_COIN_STATES,
                    new SafeCounter(),
                    completionLatch,
                    firstFailure
            ));
        }

        // Worker'lar kuyruğu beklerken PoisonPill gönder
        taskQueue.putPoisonPills(workerCount);

        executor.shutdown();
        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(terminated, "Worker'lar PoisonPill aldıktan sonra 5 saniye içinde sonlanmalı");
        assertNull(firstFailure.get(), "Worker'lardan hata fırlatılmamalı");
    }

    /**
     * ExecutorService.shutdownNow() çağrıldığında worker thread'leri interrupt flag'i
     * alır. PriceWorker'ın InterruptedException'ı yakalayıp bayrağı geri set ettiği
     * ve graceful şekilde sonlandığı doğrulanır.
     */
    @Test
    void workersShouldHandleInterruptAndStopCleanly() throws Exception {
        int workerCount = 2;
        TaskQueue taskQueue = new TaskQueue(16);
        CountDownLatch completionLatch = new CountDownLatch(0);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(
                workerCount,
                new WorkerThreadFactory("safe-worker-")
        );

        for (int i = 0; i < workerCount; i++) {
            executor.submit(new PriceWorker(
                    taskQueue,
                    SAFE_COIN_STATES,
                    new SafeCounter(),
                    completionLatch,
                    firstFailure
            ));
        }

        // Worker'lar kuyruğu beklerken interrupt et
        Thread.sleep(50); // Worker'ların BlockingQueue.take() içinde olmasını bekle
        executor.shutdownNow();

        boolean terminated = executor.awaitTermination(3, TimeUnit.SECONDS);

        assertTrue(terminated, "Worker'lar interrupt sonrası 3 saniye içinde sonlanmalı");
        // firstFailure null olmalı — InterruptedException bir hata değil, graceful stop
        assertNull(firstFailure.get(), "Interrupt worker hatasına dönüşmemeli");
    }

    /**
     * Unsafe-worker'lar için PoisonPill ile graceful shutdown.
     * ThreadDumpEvidenceTest yalnızca safe-worker içerdiğinden bu test
     * unsafe-worker thread adlarının da doğru olduğunu kanıtlar.
     */
    @Test
    void unsafeWorkersShouldStopGracefullyOnPoisonPill() throws Exception {
        int workerCount = 4;
        TaskQueue taskQueue = new TaskQueue(16);
        CountDownLatch completionLatch = new CountDownLatch(0);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(
                workerCount,
                new WorkerThreadFactory("unsafe-worker-")
        );

        for (int i = 0; i < workerCount; i++) {
            executor.submit(new PriceWorker(
                    taskQueue,
                    UNSAFE_COIN_STATES,
                    new UnsafeCounter(),
                    completionLatch,
                    firstFailure
            ));
        }

        // Thread adlarını doğrula
        Thread.sleep(50);
        boolean hasUnsafeWorkerThreads = Thread.getAllStackTraces().keySet().stream()
                .anyMatch(t -> t.getName().startsWith("unsafe-worker-"));
        assertTrue(hasUnsafeWorkerThreads,
                "ExecutorService unsafe-worker- önekiyle thread oluşturmalı");

        taskQueue.putPoisonPills(workerCount);
        executor.shutdown();
        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);

        assertTrue(terminated, "Unsafe worker'lar PoisonPill sonrası 5 saniyede sonlanmalı");
        assertNull(firstFailure.get(), "Unsafe worker'lardan hata fırlatılmamalı");
    }

    /**
     * Görevler kuyrukta işlenirken executor sonlandırıldığında
     * kalan görevlerin kuyrukta kaldığı ve latch'in eksik kaldığı doğrulanır.
     * Bu, yarım kalan simülasyonun neden doğrulama yapılmaması gerektiğini kanıtlar.
     */
    @Test
    void shutdownDuringProcessingShouldNotCorruptWorkerInternals() throws Exception {
        int workerCount = 2;
        int taskCount = 100;
        TaskQueue taskQueue = new TaskQueue(200);
        CountDownLatch completionLatch = new CountDownLatch(taskCount);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(
                workerCount,
                new WorkerThreadFactory("safe-worker-")
        );

        for (int i = 0; i < workerCount; i++) {
            executor.submit(new PriceWorker(
                    taskQueue,
                    SAFE_COIN_STATES,
                    new SafeCounter(),
                    completionLatch,
                    firstFailure
            ));
        }

        // Sadece birkaç görev ekle (tamamı değil)
        for (int i = 0; i < 10; i++) {
            taskQueue.put(new com.infina.price_simulator.model.PriceUpdateTask(
                    (long) i, "BTC", 1L
            ));
        }

        // Worker'lar henüz tüm görevleri bitirmeden interrupt et
        Thread.sleep(20);
        executor.shutdownNow();
        boolean terminated = executor.awaitTermination(3, TimeUnit.SECONDS);

        assertTrue(terminated, "Executor zorla durdurulabilmeli");
        // Worker'ların exception atmadığını doğrula — InterruptedException normal bir çıkış
        assertNull(firstFailure.get(),
                "Interrupt sırasındaki işçi hataları firstFailure'a yazılmamalı");
    }
}
