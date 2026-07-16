package com.infina.price_simulator.api.dto;

import java.util.List;

public record StatsResponse(
        long seed,
        long submittedUpdates,
        long unsafeProcessedUpdates,
        long safeProcessedUpdates,
        int workers,
        long unsafeElapsedMs,
        long safeElapsedMs,
        long unsafeThroughputPerSec,
        long safeThroughputPerSec,
        boolean safeInvariantPassed,
        List<CoinComparisonResponse> coins
) {
}