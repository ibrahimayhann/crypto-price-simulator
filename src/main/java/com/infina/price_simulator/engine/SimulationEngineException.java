package com.infina.price_simulator.engine;

public class SimulationEngineException extends RuntimeException {

    public SimulationEngineException(String message) {
        super(message);
    }

    public SimulationEngineException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}