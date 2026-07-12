package com.infina.price_simulator.metrics;

import com.infina.price_simulator.model.ExpectedValues;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.state.CoinState;
import com.infina.price_simulator.state.SafeCoinState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExpectedCalculatorTest {

    @Test
    void shouldCalculateExpectedValuesCorrectly() {

        List<CoinState> coins = List.of(
                new SafeCoinState("BTC", 100),
                new SafeCoinState("ETH", 200)
        );

        List<PriceUpdateTask> tasks = List.of(
                new PriceUpdateTask(1, "BTC", 20),
                new PriceUpdateTask(2, "BTC", -10),
                new PriceUpdateTask(3, "ETH", 50)
        );

        Map<String, ExpectedValues> expected =
                ExpectedCalculator.calculate(coins, tasks);

        assertEquals(110, expected.get("BTC").expectedPrice());
        assertEquals(2, expected.get("BTC").expectedUpdateCount());

        assertEquals(250, expected.get("ETH").expectedPrice());
        assertEquals(1, expected.get("ETH").expectedUpdateCount());
    }
}