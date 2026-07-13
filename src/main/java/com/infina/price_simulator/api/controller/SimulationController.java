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

    //  Issue #9'u bitirildiğinde buradaki yorum satırlarını kaldırılacak:

    // private final SimulationService simulationService;
    //
    // public SimulationController(SimulationService simulationService) {
    //     this.simulationService = simulationService;
    // }

    @PostMapping("/simulate")
    public ResponseEntity<StatsResponse> simulate(
            @RequestParam @Min(1) @Max(100000) int updates,
            @RequestParam @Min(1) @Max(16) int workers,
            @RequestParam(required = false) Long seed) {

        // StatsResponse response = simulationService.runSimulation(updates, workers, seed);
        // return ResponseEntity.ok(response);

        // Şimdilik hata vermemesi için boş dönüyoruz:
        return ResponseEntity.ok(null);
    }

    @GetMapping("/coins")
    public ResponseEntity<List<CoinResponse>> getCoins() {

        // return ResponseEntity.ok(simulationService.getLatestCoins());

        return ResponseEntity.ok(null);
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {

        // return ResponseEntity.ok(simulationService.getLatestStats());

        return ResponseEntity.ok(null);
    }
}