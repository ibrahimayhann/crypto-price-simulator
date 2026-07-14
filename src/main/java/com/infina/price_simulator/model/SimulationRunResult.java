package com.infina.price_simulator.model;

import com.infina.price_simulator.engine.SimulationMode;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * Güvenli veya güvenli olmayan bir engine çalıştırması sonucunda üretilen
 * değiştirilemez sonucu temsil eder.
 */

public record SimulationRunResult(
        SimulationMode mode,
        int submittedUpdates,
        long processedUpdates,
        int workers,
        long elapsedNanos,
        List<CoinRunSnapshot> coins
) {

    public SimulationRunResult {
        Objects.requireNonNull(mode, "Simulation mode must not be null.");
        Objects.requireNonNull(coins, "Coin results must not be null.");

        coins = List.copyOf(coins);

        if (submittedUpdates < 1) {
            throw new IllegalArgumentException(
                    "Submitted updates must be positive."
            );
        }

        if (workers < 1) {
            throw new IllegalArgumentException(
                    "Worker count must be positive."
            );
        }

        if (elapsedNanos < 0) {
            throw new IllegalArgumentException(
                    "Elapsed time must not be negative."
            );
        }
    }

    public long elapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
    }

/**
 * İşlem hacmi hesabında, bilinçli olarak iş parçacığı güvenli olmayan işlenmiş görev
 * sayacı yerine gönderilen görev sayısı kullanılır. Gönderilen tüm görevler gerçek
 * iş yükünü temsil ederken, güvenli olmayan sayaç yarış koşulları nedeniyle bazı
 * artışları kaybedebilir.
 */

    public double throughputPerSecond() {
        if (elapsedNanos == 0L) {
            return 0.0;
        }

        return submittedUpdates
                * 1_000_000_000.0
                / elapsedNanos;
    }
}