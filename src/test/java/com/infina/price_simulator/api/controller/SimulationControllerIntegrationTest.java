package com.infina.price_simulator.api.controller;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /simulate, /coins ve /stats uçlarının gerçek SimulationService ile uçtan uca
 * çalıştığını doğrular. Testler sıralı çalışır çünkü /coins ve /stats,
 * /simulate'in bıraktığı son sonuca bakar.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void invalidUpdatesReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "0")
                        .param("workers", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(2)
    void invalidWorkersReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "500")
                        .param("workers", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(3)
    void simulateRunsAndReturnsInvariantPassedStats() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "500")
                        .param("workers", "4")
                        .param("seed", "42")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seed").value(42))
                .andExpect(jsonPath("$.submittedUpdates").value(500))
                .andExpect(jsonPath("$.safeProcessedUpdates").value(500))
                .andExpect(jsonPath("$.safeInvariantPassed").value(true))
                .andExpect(jsonPath("$.coins", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    @Order(4)
    void statsReturnsLastCompletedSimulation() throws Exception {
        mockMvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seed").value(42));
    }

    @Test
    @Order(5)
    void coinsReturnsLastSafeSnapshot() throws Exception {
        mockMvc.perform(get("/coins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)));
    }
}
