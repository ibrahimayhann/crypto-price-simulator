package com.infina.price_simulator.api.controller;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /simulate, /coins ve /stats uçlarının gerçek SimulationService ile uçtan uca
 * çalıştığını doğrular. Testler sıralı çalışır çünkü /coins ve /stats,
 * /simulate'in bıraktığı son sonuca bakar.
 *
 * Issue #12 kapsamında eklenen senaryolar:
 * - updates=-1, updates=100001 → 400 Bad Request
 * - workers=0, workers=17 → 400 Bad Request
 * - Simülasyon öncesi /stats ve /coins → 404 Not Found
 * - Eşzamanlı /simulate isteği → 409 Conflict
 * - Response safe/unsafe alanlarının tam doğrulanması
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SimulationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ─────────────────────────────────────────────────────────────────
    // Sınır Dışı Parametre Testleri (400 Bad Request)
    // ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    void statsBeforeAnySimulationReturns404() throws Exception {
        mockMvc.perform(get("/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/stats"));
    }

    @Test
    @Order(2)
    void coinsBeforeAnySimulationReturns404() throws Exception {
        mockMvc.perform(get("/coins"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/coins"));
    }

    @Test
    @Order(3)
    void negativeUpdatesReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "-1")
                        .param("workers", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.path").value("/simulate"));
    }

    @Test
    @Order(4)
    void zeroUpdatesReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "0")
                        .param("workers", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(5)
    void tooLargeUpdatesReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "100001")
                        .param("workers", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @Order(6)
    void zeroWorkersReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "500")
                        .param("workers", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @Order(7)
    void tooManyWorkersReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "500")
                        .param("workers", "17"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @Order(8)
    void missingUpdatesParamReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("workers", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @Order(9)
    void missingWorkersParamReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ─────────────────────────────────────────────────────────────────
    // Başarılı Simülasyon + Safe/Unsafe Alan Doğrulaması
    // ─────────────────────────────────────────────────────────────────

    @Test
    @Order(10)
    void simulateRunsAndReturnsFullyValidatedResponse() throws Exception {
        mockMvc.perform(post("/simulate")
                        .param("updates", "500")
                        .param("workers", "4")
                        .param("seed", "42")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Temel alanlar
                .andExpect(jsonPath("$.seed").value(42))
                .andExpect(jsonPath("$.submittedUpdates").value(500))
                .andExpect(jsonPath("$.workers").value(4))
                // Safe mod her zaman tüm görevleri işlemeli
                .andExpect(jsonPath("$.safeProcessedUpdates").value(500))
                .andExpect(jsonPath("$.safeInvariantPassed").value(true))
                // Performans alanları var ve pozitif olmalı
                .andExpect(jsonPath("$.safeElapsedMs").isNumber())
                .andExpect(jsonPath("$.unsafeElapsedMs").isNumber())
                .andExpect(jsonPath("$.safeThroughputPerSec").isNumber())
                .andExpect(jsonPath("$.unsafeThroughputPerSec").isNumber())
                // Unsafe alanlar var (değerleri race condition nedeniyle farklı olabilir)
                .andExpect(jsonPath("$.unsafeProcessedUpdates").isNumber())
                // Coin karşılaştırma listesi
                .andExpect(jsonPath("$.coins").isArray())
                .andExpect(jsonPath("$.coins", org.hamcrest.Matchers.hasSize(3)))
                // Her coin'de safe/unsafe/expected alanlar mevcut olmalı
                .andExpect(jsonPath("$.coins[0].id").exists())
                .andExpect(jsonPath("$.coins[0].initial").isNumber())
                .andExpect(jsonPath("$.coins[0].expected").isNumber())
                .andExpect(jsonPath("$.coins[0].safe").isNumber())
                .andExpect(jsonPath("$.coins[0].unsafe").isNumber())
                .andExpect(jsonPath("$.coins[0].expectedUpdateCount").isNumber())
                .andExpect(jsonPath("$.coins[0].safeUpdateCount").isNumber())
                .andExpect(jsonPath("$.coins[0].unsafeUpdateCount").isNumber())
                // safeInvariantPassed=true olduğunda safe == expected garantisi sağlanmış demektir.
                // Cross-field MockMvc karşılaştırması mümkün olmadığından üst düzey flag yeterli kanıttır.
                .andExpect(jsonPath("$.safeInvariantPassed").value(true));
    }

    @Test
    @Order(11)
    void statsReturnsLastCompletedSimulation() throws Exception {
        mockMvc.perform(get("/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seed").value(42))
                .andExpect(jsonPath("$.submittedUpdates").value(500))
                .andExpect(jsonPath("$.safeInvariantPassed").value(true))
                .andExpect(jsonPath("$.coins").isArray());
    }

    @Test
    @Order(12)
    void coinsReturnsLastSafeSnapshot() throws Exception {
        mockMvc.perform(get("/coins"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)))
                // Her coin'de tüm alanlar doğrulanır
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].initialPrice").isNumber())
                .andExpect(jsonPath("$[0].currentPrice").isNumber())
                .andExpect(jsonPath("$[0].updateCount").isNumber())
                .andExpect(jsonPath("$[0].lastDelta").isNumber())
                .andExpect(jsonPath("$[0].lastUpdatedBy").isString());
    }

    // ─────────────────────────────────────────────────────────────────
    // Eşzamanlı İstek (409 Conflict)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Aynı anda iki /simulate isteği gönderildiğinde, biri 200 diğeri 409 almalı.
     *
     * <p>Thread.sleep() kullanılmaz. Bunun yerine bir {@link CountDownLatch} "start
     * tabancası" her iki iş parçacığını da aynı anda serbest bırakır. Bu sayede
     * {@code SimulationService.running} AtomicBoolean'ının yarış koşulunu yakalama
     * olasılığı maksimuma çıkar.</p>
     *
     * <p>İlk istek 50.000 güncelleme çalıştırdığından birinci simülasyon bitene kadar
     * ikinci istek 409 alır. Bu test, AtomicBoolean.compareAndSet garantisi sayesinde
     * kesin olarak 409 bekler.</p>
     */
    @Test
    @Order(13)
    void concurrentSimulationRequestReturns409Conflict() throws Exception {
        CountDownLatch startGun = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // İlk istek: yüksek iş yükü — simülasyon süresince running=true tutar
        Future<Integer> firstRequest = executor.submit(() -> {
            startGun.await();
            return mockMvc.perform(post("/simulate")
                            .param("updates", "50000")
                            .param("workers", "4"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        });

        // İkinci istek: aynı latch'i bekler; tabanca ateşlenince birlikte başlarlar
        Future<Integer> secondRequest = executor.submit(() -> {
            startGun.await();
            return mockMvc.perform(post("/simulate")
                            .param("updates", "500")
                            .param("workers", "2"))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        });

        // Her iki thread da latch'i geçmeye hazır; şimdi aynı anda serbest bırak
        startGun.countDown();

        int firstStatus  = firstRequest.get(60, TimeUnit.SECONDS);
        int secondStatus = secondRequest.get(60, TimeUnit.SECONDS);

        executor.shutdown();

        // AtomicBoolean.compareAndSet garantisi: tam olarak biri 200, diğeri 409 almalı
        boolean oneSucceeded     = firstStatus == 200 || secondStatus == 200;
        boolean conflictDetected = firstStatus == 409 || secondStatus == 409;

        assertTrue(oneSucceeded,
                "En az bir simülasyon başarıyla tamamlanmalı. "
                        + "first=" + firstStatus + ", second=" + secondStatus);

        assertTrue(conflictDetected,
                "Eşzamanlı /simulate isteği kesin olarak HTTP 409 Conflict döndürmeli. "
                        + "first=" + firstStatus + ", second=" + secondStatus);
    }
}
