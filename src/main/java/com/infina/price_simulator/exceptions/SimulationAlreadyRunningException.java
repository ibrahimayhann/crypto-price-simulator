package com.infina.price_simulator.exceptions;

public class SimulationAlreadyRunningException extends RuntimeException {

    public SimulationAlreadyRunningException(String message) {
        super(message);
    }
}