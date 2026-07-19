package com.infina.price_simulator.api.mapper;

import com.infina.price_simulator.api.dto.CoinComparisonResponse;
import com.infina.price_simulator.api.dto.CoinResponse;
import com.infina.price_simulator.api.dto.StatsResponse;
import com.infina.price_simulator.model.CoinComparison;
import com.infina.price_simulator.model.CoinRunSnapshot;
import com.infina.price_simulator.model.SimulationStats;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SimulationResponseMapper {

    public StatsResponse toStatsResponse(SimulationStats stats) {
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

    public List<CoinResponse> toCoinResponses(List<CoinRunSnapshot> coins) {
        return coins.stream().map(this::toCoinResponse).toList();
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
