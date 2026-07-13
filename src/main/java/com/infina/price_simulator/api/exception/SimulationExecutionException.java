package com.infina.price_simulator.api.exception;

public class SimulationExecutionException extends RuntimeException {
    public SimulationExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}