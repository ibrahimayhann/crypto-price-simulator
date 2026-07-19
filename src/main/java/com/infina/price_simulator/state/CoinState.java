package com.infina.price_simulator.state;

import lombok.Getter;

@Getter
public abstract class CoinState {

    protected final String id;
    protected final long initialPrice;

    protected long currentPrice;
    protected long updateCount;
    protected long lastDelta;
    protected String lastUpdatedBy;

    protected CoinState(String id, long initialPrice) {
        this.id = id;
        this.initialPrice = initialPrice;
        this.currentPrice = initialPrice;
        this.updateCount = 0;
        this.lastDelta = 0;
        this.lastUpdatedBy = null;
    }

    protected void updateState(long delta) {
        currentPrice += delta;
        updateCount++;
        lastDelta = delta;
        lastUpdatedBy = Thread.currentThread().getName();
    }

    public abstract void applyDelta(long delta);
}
