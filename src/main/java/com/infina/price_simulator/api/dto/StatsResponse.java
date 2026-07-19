package com.infina.price_simulator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Tamamlanan simülasyonun özet istatistikleri")
public record StatsResponse(

        @Schema(description = "Simülasyonda kullanılan seed değeri", example = "42")
        long seed,

        @Schema(description = "Üretilen toplam güncelleme görevi sayısı", example = "500")
        long submittedUpdates,

        @Schema(description = "Unsafe modda işlenen güncelleme sayısı (race condition nedeniyle eksik olabilir)", example = "487")
        long unsafeProcessedUpdates,

        @Schema(description = "Safe modda işlenen güncelleme sayısı (her zaman submittedUpdates'e eşit olmalı)", example = "500")
        long safeProcessedUpdates,

        @Schema(description = "Kullanılan worker thread sayısı", example = "4")
        int workers,

        @Schema(description = "Unsafe modun toplam çalışma süresi (milisaniye)", example = "12")
        long unsafeElapsedMs,

        @Schema(description = "Safe modun toplam çalışma süresi (milisaniye)", example = "18")
        long safeElapsedMs,

        @Schema(description = "Unsafe modun saniyedeki güncelleme throughput'u", example = "41666")
        long unsafeThroughputPerSec,

        @Schema(description = "Safe modun saniyedeki güncelleme throughput'u", example = "27777")
        long safeThroughputPerSec,

        @Schema(description = "Safe modun deterministik invariantı sağlayıp sağlamadığı", example = "true")
        boolean safeInvariantPassed,

        @Schema(description = "Her coin için unsafe/safe/expected karşılaştırma listesi")
        List<CoinComparisonResponse> coins

) {}