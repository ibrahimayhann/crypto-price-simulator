package com.infina.price_simulator.model;

import java.util.List;

/**
 * Simülasyonda kullanılan coin'lerin ve başlangıç fiyatlarının tek kaynağı.
 * Engine ve Service aynı tanımları burada paylaşır.
 */
public final class CoinCatalog {

    public static final List<CoinDefinition> DEFINITIONS = List.of(
            new CoinDefinition("BTC", 60_000L),
            new CoinDefinition("ETH", 3_000L),
            new CoinDefinition("SOL", 150L)
    );

    private CoinCatalog() {
    }
}
