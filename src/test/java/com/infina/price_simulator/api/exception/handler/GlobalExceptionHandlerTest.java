package com.infina.price_simulator.api.exception.handler;

import com.infina.price_simulator.api.exception.SimulationAlreadyRunningException;
import com.infina.price_simulator.api.exception.SimulationNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler'ı gerçek Spring context'i ayağa kaldırmadan,
 * her exception tipini fırlatan sahte bir controller üzerinden doğrular.
 */
class GlobalExceptionHandlerTest {

    @RestController
    static class ThrowingTestController {

        @GetMapping("/test/already-running")
        void alreadyRunning() {
            throw new SimulationAlreadyRunningException("A simulation is already running.");
        }

        @GetMapping("/test/not-found")
        void notFound() {
            throw new SimulationNotFoundException("No completed simulation result found.");
        }

        @GetMapping("/test/missing-param")
        void missingParam(@RequestParam int updates) {
        }

        @GetMapping("/test/constraint-violation")
        void constraintViolation() {
            throw new ConstraintViolationException("updates: must be greater than or equal to 1", Set.of());
        }

        @GetMapping("/test/generic")
        void generic() {
            throw new IllegalStateException("db connection string leaked: jdbc://internal-secret");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingTestController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void alreadyRunningReturns409WithConflictBody() throws Exception {
        mockMvc.perform(get("/test/already-running"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("A simulation is already running."))
                .andExpect(jsonPath("$.path").value("/test/already-running"));
    }

    @Test
    void notFoundReturns404WithNotFoundBody() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No completed simulation result found."));
    }

    @Test
    void missingParamReturns400BadRequest() throws Exception {
        mockMvc.perform(get("/test/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void constraintViolationReturns400BadRequest() throws Exception {
        mockMvc.perform(get("/test/constraint-violation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void genericExceptionReturns500WithoutLeakingExceptionMessage() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.path").value("/test/generic"));
    }
}
