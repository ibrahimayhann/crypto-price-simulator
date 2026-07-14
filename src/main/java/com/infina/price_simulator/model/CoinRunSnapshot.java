package com.infina.price_simulator.model;


/**
 * Bir simülasyon çalıştırması sonrasında coin'in değiştirilemez nihai durumunu temsil eder.
 */


public record CoinRunSnapshot(
        String id,
        long initialPrice,
        long currentPrice,
        long updateCount,
        long lastDelta,
        String lastUpdatedBy
) {
}