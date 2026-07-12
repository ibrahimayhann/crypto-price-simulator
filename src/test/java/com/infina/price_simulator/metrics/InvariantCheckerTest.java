package com.infina.price_simulator.metrics;

import com.infina.price_simulator.model.ExpectedValues;
import com.infina.price_simulator.state.CoinState;
import com.infina.price_simulator.state.SafeCoinState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InvariantCheckerTest {

    @Test
    void shouldReturnTrueWhenInvariantsHold() {

        CoinState btc = new SafeCoinState("BTC", 100);
        CoinState eth = new SafeCoinState("ETH", 200);

        btc.applyDelta(20);
        eth.applyDelta(50);

        Map<String, ExpectedValues> expected = Map.of(
                "BTC", new ExpectedValues(120, 1),
                "ETH", new ExpectedValues(250, 1)
        );

        assertTrue(
                InvariantChecker.check(
                        List.of(btc, eth),
                        expected
                )
        );
    }

    @Test
    void shouldReturnFalseWhenPriceDoesNotMatch() {

        CoinState btc = new SafeCoinState("BTC", 100);

        btc.applyDelta(20);

        Map<String, ExpectedValues> expected = Map.of(
                "BTC", new ExpectedValues(999, 1)
        );

        assertFalse(
                InvariantChecker.check(
                        List.of(btc),
                        expected
                )
        );
    }

    @Test
    void shouldReturnFalseWhenUpdateCountDoesNotMatch() {

        CoinState btc = new SafeCoinState("BTC", 100);

        btc.applyDelta(20);

        Map<String, ExpectedValues> expected = Map.of(
                "BTC", new ExpectedValues(120, 5)
        );

        assertFalse(
                InvariantChecker.check(
                        List.of(btc),
                        expected
                )
        );
    }
}