package com.infina.price_simulator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "API hata yanıtı")
public record ErrorResponse(

        @Schema(description = "Hatanın oluştuğu zaman damgası", example = "2024-01-15T10:30:00")
        LocalDateTime timestamp,

        @Schema(description = "HTTP durum kodu", example = "400")
        int status,

        @Schema(description = "HTTP hata açıklaması", example = "Bad Request")
        String error,

        @Schema(description = "Hatanın ayrıntılı açıklaması", example = "simulate.updates: must be greater than or equal to 1")
        String message,

        @Schema(description = "Hatanın oluştuğu endpoint yolu", example = "/simulate")
        String path

) {}