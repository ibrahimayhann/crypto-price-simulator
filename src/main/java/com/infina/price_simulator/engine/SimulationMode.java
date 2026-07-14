package com.infina.price_simulator.engine;


/**
 * Bir simülasyon çalıştırmasının iş parçacığı güvenli mi yoksa bilinçli olarak
 * güvenli olmayan durum implementasyonlarını mı kullandığını belirtir.
 */

public enum SimulationMode {

    SAFE("safe-worker-"),
    UNSAFE("unsafe-worker-");

    private final String threadNamePrefix;

    SimulationMode(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }
}