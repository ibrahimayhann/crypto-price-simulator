package com.infina.price_simulator.api.exception;

public class SimulationNotFoundException extends RuntimeException {

    public SimulationNotFoundException(String message) {
        super(message);
    }
}