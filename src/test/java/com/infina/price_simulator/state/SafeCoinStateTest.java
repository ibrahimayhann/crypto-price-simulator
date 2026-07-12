package com.infina.price_simulator.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SafeCoinStateTest {

    @Test
    void shouldApplyDeltaCorrectly() {

        SafeCoinState coin =
                new SafeCoinState("BTC", 100);

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

        SafeCoinState coin = new SafeCoinState("BTC", 100);

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