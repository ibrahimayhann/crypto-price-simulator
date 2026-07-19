package com.infina.price_simulator.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Safe modun son anlık görüntüsündeki tek bir coin bilgisi")
public record CoinResponse(

        @Schema(description = "Coin sembolü", example = "BTC")
        String id,

        @Schema(description = "Simülasyon başlangıcındaki fiyat", example = "60000")
        long initialPrice,

        @Schema(description = "Simülasyon sonundaki mevcut fiyat", example = "60150")
        long currentPrice,

        @Schema(description = "Bu coin için işlenen güncelleme sayısı", example = "167")
        long updateCount,

        @Schema(description = "Son uygulanan fiyat değişim miktarı", example = "5")
        long lastDelta,

        @Schema(description = "Bu coin'i en son güncelleyen thread adı", example = "safe-worker-3")
        String lastUpdatedBy

) {}