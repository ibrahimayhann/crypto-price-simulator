package com.infina.price_simulator.state;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SafeCoinStateTest {

    @Test
    void shouldApplyDeltaCorrectly() {

        SafeCoinState coin =
                new SafeCoinState("BTC", 100);

        coin.applyDelta(25);

        assertEquals("BTC", coin.getId());
        assertEquals(100, coin.getInitialPrice());
        assertEquals(125, coin.getCurrentPrice());
        assertEquals(1, coin.getUpdateCount());
        assertEquals(25, coin.getLastDelta());
        assertEquals(
                Thread.currentThread().getName(),
                coin.getLastUpdatedBy()
        );
    }

    @Test
    void shouldApplyMultipleDeltasCorrectly() {

        SafeCoinState coin = new SafeCoinState("BTC", 100);

        coin.applyDelta(20);
        coin.applyDelta(-10);
        coin.applyDelta(40);

        assertEquals(150, coin.getCurrentPrice());
        assertEquals(3, coin.getUpdateCount());
        assertEquals(40, coin.getLastDelta());
        assertEquals(
                Thread.currentThread().getName(),
                coin.getLastUpdatedBy()
        );
    }



    /**
     * 8 thread aynı anda SafeCoinState.applyDelta(1) çağırır.
     * ReentrantLock koruması sayesinde:
     * - currentPrice = initialPrice + (threadCount * deltasPerThread) olmalı
     * - updateCount = threadCount * deltasPerThread olmalı
     * Herhangi bir kayıp thread-safety ihlalini gösterir.
     */
    @Test
    void shouldMaintainConsistencyUnderConcurrentApplyDelta() throws InterruptedException {
        int threadCount = 8;
        int deltasPerThread = 1_000;
        long initialPrice = 10_000L;
        long delta = 1L;

        SafeCoinState coin = new SafeCoinState("ETH", initialPrice);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Hepsi aynı anda başlasın
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int j = 0; j < deltasPerThread; j++) {
                    coin.applyDelta(delta);
                }
                doneLatch.countDown();
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(15, TimeUnit.SECONDS), "Thread'ler zamanında tamamlanmalı");
        executor.shutdown();

        long expectedUpdates = (long) threadCount * deltasPerThread;
        long expectedPrice = initialPrice + expectedUpdates * delta;

        assertEquals(expectedUpdates, coin.getUpdateCount(),
                "SafeCoinState concurrent altında güncelleme kaybetmemeli");
        assertEquals(expectedPrice, coin.getCurrentPrice(),
                "SafeCoinState concurrent altında fiyat tutarlı olmalı");
    }

    /**
     * SafeCoinState ve UnsafeCoinState aynı koşulda çalıştırılır.
     * Safe versiyonun her zaman doğru sonuç vereceği, Unsafe'in
     * race condition nedeniyle hatalı sonuç üretme ihtimalinin olduğu gösterilir.
     *
     * Not: Bu test Unsafe'in mutlaka hatalı olacağını garanti etmez
     * (scheduler'a bağlı), ama Safe'in DAIMA doğru olduğunu kanıtlar.
     */
    @Test
    void safeStateShouldAlwaysPassInvariantWhereasUnsafeMayNot() throws InterruptedException {
        int threadCount = 6;
        int deltasPerThread = 2_000;
        long initialPrice = 5_000L;
        long delta = 1L;

        SafeCoinState safe = new SafeCoinState("BTC", initialPrice);
        UnsafeCoinState unsafe = new UnsafeCoinState("BTC", initialPrice);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount * 2);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < deltasPerThread; j++) safe.applyDelta(delta);
                doneLatch.countDown();
            });
            executor.submit(() -> {
                try { startLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                for (int j = 0; j < deltasPerThread; j++) unsafe.applyDelta(delta);
                doneLatch.countDown();
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(20, TimeUnit.SECONDS));
        executor.shutdown();

        long expectedPrice = initialPrice + (long) threadCount * deltasPerThread * delta;
        long expectedCount = (long) threadCount * deltasPerThread;

        // Safe DAIMA geçmeli
        assertEquals(expectedPrice, safe.getCurrentPrice(),
                "SafeCoinState invariantı her zaman sağlamalı");
        assertEquals(expectedCount, safe.getUpdateCount(),
                "SafeCoinState güncelleme sayısı her zaman doğru olmalı");

        // Unsafe büyük olasılıkla geçmez — en azından Safe'in geçtiğini belgeliyoruz
        boolean unsafeCorrect = unsafe.getCurrentPrice() == expectedPrice
                && unsafe.getUpdateCount() == expectedCount;
        System.out.println("[Kanıt] Unsafe invariant passed: " + unsafeCorrect
                + " | Safe invariant passed: true (assert edildi)");
    }
}
