package com.infina.price_simulator.model;

import java.util.List;

/**
 * Bir simülasyon çalıştırmasının expected/unsafe/safe karşılaştırmasını
 * ve metriklerini taşıyan değiştirilemez sonuç.
 */
public record SimulationStats(
        int submittedUpdates,
        int workers,
        long seed,
        long unsafeElapsedMillis,
        long safeElapsedMillis,
        double unsafeThroughputPerSecond,
        double safeThroughputPerSecond,
        long unsafeProcessedUpdates,
        long safeProcessedUpdates,
        List<CoinComparison> coins,
        boolean safeInvariantPassed
) {
    public SimulationStats {
        coins = List.copyOf(coins);
    }
}