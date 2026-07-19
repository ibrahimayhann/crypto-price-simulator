package com.infina.price_simulator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tek bir coin için unsafe / safe / expected karşılaştırması")
public record CoinComparisonResponse(

        @Schema(description = "Coin sembolü", example = "BTC")
        String id,

        @Schema(description = "Simülasyon başlangıcındaki fiyat", example = "60000")
        long initial,

        @Schema(description = "Deterministik olarak hesaplanan beklenen son fiyat", example = "60150")
        long expected,

        @Schema(description = "Unsafe modun ürettiği son fiyat (race condition nedeniyle beklenen'den farklı olabilir)", example = "59980")
        long unsafe,

        @Schema(description = "Safe modun ürettiği son fiyat (her zaman expected'a eşit olmalı)", example = "60150")
        long safe,

        @Schema(description = "Bu coin için beklenen güncelleme sayısı", example = "167")
        long expectedUpdateCount,

        @Schema(description = "Unsafe modda bu coin için işlenen güncelleme sayısı", example = "162")
        long unsafeUpdateCount,

        @Schema(description = "Safe modda bu coin için işlenen güncelleme sayısı", example = "167")
        long safeUpdateCount

) {}