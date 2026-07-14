package com.infina.price_simulator.api.controller;

import com.infina.price_simulator.api.dto.CoinResponse;
import com.infina.price_simulator.api.dto.StatsResponse;
// Ekip arkadaşın servisi yazdığında bu import'u açacaksın:
// import com.infina.price_simulator.service.SimulationService;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@RestController
@Validated
public class SimulationController {


    
    private final SimulationService simulationService;


     public SimulationController(SimulationService simulationService) {
         this.simulationService = simulationService;
     }

    @PostMapping("/simulate")
    public ResponseEntity<StatsResponse> simulate(
            @RequestParam @Min(1) @Max(100000) int updates,
            @RequestParam @Min(1) @Max(16) int workers,
            @RequestParam(required = false) Long seed) {

         StatsResponse response = simulationService.runSimulation(updates, workers, seed);
         return ResponseEntity.ok(response);
    }

    @GetMapping("/coins")
    public ResponseEntity<List<CoinResponse>> getCoins() {
        List<CoinResponse> response = simulationService.getCoins();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        StatsResponse response = simulationService.getStats();
        return ResponseEntity.ok(response);
    }
}