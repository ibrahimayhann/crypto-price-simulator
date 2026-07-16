package com.infina.price_simulator.model;

/**
 * Bir coin için expected, unsafe ve safe sonuçlarının karşılaştırmasını taşır.
 */
public record CoinComparison(
        String id,
        long initialPrice,
        long expectedPrice,
        long unsafePrice,
        long safePrice,
        long expectedUpdateCount,
        long unsafeUpdateCount,
        long safeUpdateCount
) {
}