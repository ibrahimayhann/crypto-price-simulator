package com.infina.price_simulator.api.dto;

public record CoinResponse(
        String id,
        long initialPrice,
        long currentPrice,
        long updateCount,
        long lastDelta,
        String lastUpdatedBy
) {
}