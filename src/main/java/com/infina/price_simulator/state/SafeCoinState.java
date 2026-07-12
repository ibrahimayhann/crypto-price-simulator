package com.infina.price_simulator.state;

import java.util.concurrent.locks.ReentrantLock;

public class SafeCoinState extends CoinState{
    private final ReentrantLock lock = new ReentrantLock();

    public SafeCoinState(String id, long initialPrice) {
        super(id, initialPrice);
    }

    @Override
    public void applyDelta(long delta) {
        lock.lock();

        try {
            updateState(delta);

        } finally {
            lock.unlock();
        }
    }
}
