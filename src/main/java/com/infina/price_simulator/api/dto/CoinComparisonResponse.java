package com.infina.price_simulator.api.dto;

public record CoinComparisonResponse(
        String id,
        long initial,
        long expected,
        long unsafe,
        long safe,
        long expectedUpdateCount,
        long unsafeUpdateCount,
        long safeUpdateCount
) {
}