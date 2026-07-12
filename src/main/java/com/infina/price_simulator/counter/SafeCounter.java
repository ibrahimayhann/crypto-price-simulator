package com.infina.price_simulator.counter;

import java.util.concurrent.atomic.AtomicLong;

public class SafeCounter implements Counter{

    private final AtomicLong value = new AtomicLong();

    @Override
    public void increment() {
        value.incrementAndGet();
    }

    @Override
    public long get() {
        return value.get();
    }
}
