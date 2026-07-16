package com.infina.price_simulator.api.exception;

public class SimulationAlreadyRunningException extends RuntimeException {
    public SimulationAlreadyRunningException(String message) {
        super(message);
    }
}