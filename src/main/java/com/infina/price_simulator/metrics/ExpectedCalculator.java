package com.infina.price_simulator.metrics;

import com.infina.price_simulator.model.ExpectedValues;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.state.CoinState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ExpectedCalculator {

    private ExpectedCalculator() {
    }


     //Her coin için beklenen son fiyatı ve beklenen update sayısını hesaplar.

    public static Map<String, ExpectedValues> calculate(
            List<CoinState> coins,
            List<PriceUpdateTask> tasks
    ) {

        Map<String, ExpectedValues> expected = new HashMap<>();

        // Başlangıç değerlerini oluştur
        for (CoinState coin : coins) {
            expected.put(
                    coin.getId(),
                    new ExpectedValues(
                            coin.getInitialPrice(),
                            0
                    )
            );
        }

        // Task'ları işle
        for (PriceUpdateTask task : tasks) {
            expected.computeIfPresent(
                    task.coinId(),
                    (key, values) -> new ExpectedValues(
                            values.expectedPrice() + task.delta(),
                            values.expectedUpdateCount() + 1
                    )
            );
        }

        return expected;
    }
}