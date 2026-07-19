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
 * Senkronizasyon için Thread.sleep() yerine CountDownLatch ve thread state polling kullanılır.
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
     * Belirtilen öneke sahip en az {@code expectedCount} kadar thread'in
     * WAITING durumuna geçmesini bekler (BlockingQueue.take() üzerinde bloke).
     * Thread.sleep() yerine deterministik thread-state polling kullanılır.
     */
    private static void awaitWorkersWaiting(String namePrefix, int expectedCount, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            long waitingCount = Thread.getAllStackTraces().keySet().stream()
                    .filter(t -> t.getName().startsWith(namePrefix))
                    .filter(t -> t.getState() == Thread.State.WAITING)
                    .count();
            if (waitingCount >= expectedCount) {
                return;
            }
            Thread.yield();
        }
        // Timeout doldu; test devam eder — awaitTermination ile zaten doğrulama yapılır
    }

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

        // Worker'lar BlockingQueue.take() üzerinde WAITING durumuna geçene kadar bekle
        awaitWorkersWaiting("safe-worker-", workerCount, 2_000);

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

        // Thread.sleep() yerine: worker'ların BlockingQueue.take() üzerinde WAITING durumuna
        // geçmesini deterministik olarak bekle
        awaitWorkersWaiting("safe-worker-", workerCount, 2_000);

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

        // Thread.sleep() yerine: unsafe-worker'ların WAITING durumuna geçmesini bekle
        awaitWorkersWaiting("unsafe-worker-", workerCount, 2_000);

        // Thread adlarını WAITING durumuna geçtikten sonra doğrula
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
     * worker'ların temiz şekilde çıktığı doğrulanır.
     *
     * <p>Senkronizasyon: worker'ların başladığını doğrulamak için WAITING olmayan
     * (yani aktif çalışan) thread sayısı beklenir. Thread.sleep() kullanılmaz.</p>
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

        // Worker'lar kuyruğu beklerken görevleri ekle
        for (int i = 0; i < 10; i++) {
            taskQueue.put(new com.infina.price_simulator.model.PriceUpdateTask(
                    (long) i, "BTC", 1L
            ));
        }

        // Worker'ların en az bir görevi işlemeye başladığını CountDownLatch ile doğrula.
        // Latch'in başlangıç değeri taskCount=100, dolayısıyla herhangi bir görev işlenince
        // count < taskCount olur. Bunu poll ile kontrol ediyoruz — Thread.sleep() yok.
        long deadline = System.currentTimeMillis() + 2_000;
        while (System.currentTimeMillis() < deadline) {
            if (completionLatch.getCount() < taskCount) break;
            Thread.yield();
        }

        executor.shutdownNow();
        boolean terminated = executor.awaitTermination(3, TimeUnit.SECONDS);

        assertTrue(terminated, "Executor zorla durdurulabilmeli");
        // Worker'ların exception atmadığını doğrula — InterruptedException normal bir çıkış
        assertNull(firstFailure.get(),
                "Interrupt sırasındaki işçi hataları firstFailure'a yazılmamalı");
    }
}
