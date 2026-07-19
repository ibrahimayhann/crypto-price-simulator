package com.infina.price_simulator.evidence;

import com.infina.price_simulator.engine.SimulationEngine;
import com.infina.price_simulator.engine.SimulationMode;
import com.infina.price_simulator.engine.TaskProducer;
import com.infina.price_simulator.model.CoinRunSnapshot;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.model.SimulationRunResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Calc {
    public static void main(String[] args) {
        TaskProducer taskProducer = new TaskProducer();
        SimulationEngine simulationEngine = new SimulationEngine();
        
        List<PriceUpdateTask> tasks = taskProducer.generate(50000, 42L);
        SimulationRunResult unsafeResult = simulationEngine.run(tasks, 4, SimulationMode.UNSAFE);
        SimulationRunResult safeResult = simulationEngine.run(tasks, 4, SimulationMode.SAFE);
        
        Map<String, Long> expectedPrices = new HashMap<>();
        expectedPrices.put("BTC", 60_000L);
        expectedPrices.put("ETH", 3_000L);
        expectedPrices.put("SOL", 150L);

        Map<String, Long> expectedCounts = new HashMap<>();
        expectedCounts.put("BTC", 0L);
        expectedCounts.put("ETH", 0L);
        expectedCounts.put("SOL", 0L);

        for (PriceUpdateTask task : tasks) {
            expectedPrices.computeIfPresent(task.coinId(), (coinId, price) -> price + task.delta());
            expectedCounts.computeIfPresent(task.coinId(), (coinId, count) -> count + 1L);
        }
        
        System.out.println("--- EXPECTED ---");
        for (String coin : new String[]{"BTC", "ETH", "SOL"}) {
            System.out.println(coin + " Price: " + expectedPrices.get(coin) + ", Count: " + expectedCounts.get(coin));
        }
        System.out.println("--- UNSAFE ---");
        for (CoinRunSnapshot c : unsafeResult.coins()) {
            System.out.println(c.id() + " Price: " + c.currentPrice() + ", Count: " + c.updateCount());
        }
        System.out.println("--- SAFE ---");
        for (CoinRunSnapshot c : safeResult.coins()) {
            System.out.println(c.id() + " Price: " + c.currentPrice() + ", Count: " + c.updateCount());
        }
    }
}
