package com.infina.price_simulator.counter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeCounterTest {

    @Test
    void shouldIncrementCorrectly() {

        SafeCounter counter = new SafeCounter();

        counter.increment();
        counter.increment();
        counter.increment();

        assertEquals(3, counter.get());
    }
}