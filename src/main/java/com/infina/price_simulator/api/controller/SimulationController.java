package com.infina.price_simulator.api.controller;

import com.infina.price_simulator.api.dto.CoinComparisonResponse;
import com.infina.price_simulator.api.dto.CoinResponse;
import com.infina.price_simulator.api.dto.StatsResponse;
import com.infina.price_simulator.model.CoinComparison;
import com.infina.price_simulator.model.CoinRunSnapshot;
import com.infina.price_simulator.model.SimulationStats;
import com.infina.price_simulator.service.SimulationService;

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

        // Seed verilmemişse çalıştırma anında üretilir ve cevapta geri döner
        long effectiveSeed = seed != null ? seed : System.nanoTime();

        SimulationStats stats = simulationService.runSimulation(updates, workers, effectiveSeed);
        return ResponseEntity.ok(toStatsResponse(stats));
    }

    @GetMapping("/coins")
    public ResponseEntity<List<CoinResponse>> getCoins() {
        List<CoinRunSnapshot> coins = simulationService.getCoins();
        return ResponseEntity.ok(coins.stream().map(this::toCoinResponse).toList());
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        SimulationStats stats = simulationService.getStats();
        return ResponseEntity.ok(toStatsResponse(stats));
    }

    private StatsResponse toStatsResponse(SimulationStats stats) {
        return new StatsResponse(
                stats.seed(),
                stats.submittedUpdates(),
                stats.unsafeProcessedUpdates(),
                stats.safeProcessedUpdates(),
                stats.workers(),
                stats.unsafeElapsedMillis(),
                stats.safeElapsedMillis(),
                (long) stats.unsafeThroughputPerSecond(),
                (long) stats.safeThroughputPerSecond(),
                stats.safeInvariantPassed(),
                stats.coins().stream().map(this::toCoinComparisonResponse).toList()
        );
    }

    private CoinResponse toCoinResponse(CoinRunSnapshot coin) {
        return new CoinResponse(
                coin.id(),
                coin.initialPrice(),
                coin.currentPrice(),
                coin.updateCount(),
                coin.lastDelta(),
                coin.lastUpdatedBy()
        );
    }

    private CoinComparisonResponse toCoinComparisonResponse(CoinComparison comparison) {
        return new CoinComparisonResponse(
                comparison.id(),
                comparison.initialPrice(),
                comparison.expectedPrice(),
                comparison.unsafePrice(),
                comparison.safePrice(),
                comparison.expectedUpdateCount(),
                comparison.unsafeUpdateCount(),
                comparison.safeUpdateCount()
        );
    }
}