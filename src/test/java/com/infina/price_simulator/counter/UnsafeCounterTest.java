package com.infina.price_simulator.counter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnsafeCounterTest {

    @Test
    void shouldIncrementCorrectly() {

        UnsafeCounter counter = new UnsafeCounter();

        counter.increment();
        counter.increment();
        counter.increment();

        assertEquals(3, counter.get());
    }
}