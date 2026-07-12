package com.infina.price_simulator.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnsafeCoinStateTest {

    @Test
    void shouldApplyDeltaCorrectly() {

        UnsafeCoinState coin = new UnsafeCoinState("BTC", 100);

        coin.applyDelta(25);

        assertEquals("BTC", coin.getId());
        assertEquals(100, coin.getInitialPrice());
        assertEquals(125, coin.getCurrentPrice());
        assertEquals(1, coin.getUpdateCount());
        assertEquals(25, coin.getLastDelta());
        assertEquals(
                Thread.currentThread().getName(),
                coin.getLastUpdatedBy()
        );
    }

    @Test
    void shouldApplyMultipleDeltasCorrectly() {

        UnsafeCoinState coin = new UnsafeCoinState("BTC", 100);

        coin.applyDelta(20);
        coin.applyDelta(-10);
        coin.applyDelta(40);

        assertEquals(150, coin.getCurrentPrice());
        assertEquals(3, coin.getUpdateCount());
        assertEquals(40, coin.getLastDelta());
        assertEquals(
                Thread.currentThread().getName(),
                coin.getLastUpdatedBy()
        );
    }
}