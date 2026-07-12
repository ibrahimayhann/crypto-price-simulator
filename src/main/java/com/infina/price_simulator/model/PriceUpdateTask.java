package com.infina.price_simulator.model;

public record PriceUpdateTask(
        long sequence,
        String coinId,
        long delta
) {

}
