package com.infina.price_simulator.counter;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeCounterTest {

    @Test
    void shouldIncrementCorrectly() {

        SafeCounter counter = new SafeCounter();

        counter.increment();
        counter.increment();
        counter.increment();

        assertEquals(3, counter.get());
    }

    /**
     * 10 thread aynı anda 1000'er increment yapar.
     * AtomicLong kullandığı için kayıp olmamalı → beklenen: 10_000.
     */
    @Test
    void shouldIncrementCorrectlyUnderConcurrency() throws InterruptedException {
        int threadCount = 10;
        int incrementsPerThread = 1_000;
        SafeCounter counter = new SafeCounter();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Tüm thread'ler aynı anda başlasın
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.increment();
                }
                doneLatch.countDown();
            });
        }

        startLatch.countDown(); // Başlat
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertEquals((long) threadCount * incrementsPerThread, counter.get(),
                "SafeCounter concurrent increment kaybetmemeli");
    }

    /**
     * Farklı thread'ler increment ve get aynı anda çalışır.
     * AtomicLong'un get() metodu da tutarlı olmalı.
     */
    @Test
    void shouldReadConsistentValueDuringConcurrentIncrements() throws InterruptedException {
        int writerCount = 4;
        int incrementsPerWriter = 500;
        SafeCounter counter = new SafeCounter();
        CountDownLatch doneLatch = new CountDownLatch(writerCount);
        List<Long> readValues = new CopyOnWriteArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(writerCount + 1);

        // Reader thread — increment sırasında okur
        executor.submit(() -> {
            for (int i = 0; i < 200; i++) {
                long value = counter.get();
                readValues.add(value);
                Thread.yield();
            }
        });

        // Writer thread'ler
        for (int i = 0; i < writerCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerWriter; j++) {
                    counter.increment();
                }
                doneLatch.countDown();
            });
        }

        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long finalValue = counter.get();
        assertEquals((long) writerCount * incrementsPerWriter, finalValue,
                "Tüm increment'ler tamamlandıktan sonra değer doğru olmalı");

        // Okunan değerler hiçbir zaman negatif veya final'ı aşmamalı
        for (long read : readValues) {
            assertEquals(true, read >= 0 && read <= finalValue,
                    "get() her zaman geçerli bir ara değer döndürmeli: " + read);
        }
    }
}