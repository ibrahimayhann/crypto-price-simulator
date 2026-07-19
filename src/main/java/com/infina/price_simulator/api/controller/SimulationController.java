package com.infina.price_simulator.api.controller;

import com.infina.price_simulator.api.controller.docs.SimulationApi;
import com.infina.price_simulator.api.dto.CoinResponse;
import com.infina.price_simulator.api.dto.StatsResponse;
import com.infina.price_simulator.api.mapper.SimulationResponseMapper;
import com.infina.price_simulator.service.SimulationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
public class SimulationController implements SimulationApi {

    private final SimulationService simulationService;
    private final SimulationResponseMapper responseMapper;

    @Override
    @PostMapping("/simulate")
    public ResponseEntity<StatsResponse> simulate(
            @RequestParam @Min(1) @Max(100000) int updates,
            @RequestParam @Min(1) @Max(16) int workers,
            @RequestParam(required = false) Long seed
    ) {
        long effectiveSeed = seed != null ? seed : System.nanoTime();

        return ResponseEntity.ok(responseMapper.toStatsResponse(
                simulationService.runSimulation(updates, workers, effectiveSeed)
        ));
    }

    @Override
    @GetMapping("/coins")
    public ResponseEntity<List<CoinResponse>> getCoins() {
        return ResponseEntity.ok(responseMapper.toCoinResponses(simulationService.getCoins()));
    }

    @Override
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(responseMapper.toStatsResponse(simulationService.getStats()));
    }
}
