package com.infina.price_simulator.api.controller.docs;

import com.infina.price_simulator.api.dto.CoinResponse;
import com.infina.price_simulator.api.dto.ErrorResponse;
import com.infina.price_simulator.api.dto.StatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "simulation", description = "Simulasyon baslatma ve sonuc sorgulama islemleri")
public interface SimulationApi {

    @Operation(
            summary = "Simulasyon baslat",
            description = """
                    Belirtilen sayida fiyat guncelleme gorevi uretir ve hem thread-unsafe hem de
                    thread-safe modda calistirir. Her iki modun sonuclarini karsilastirir.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Simulasyon basariyla tamamlandi",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = StatsResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "seed": 42,
                              "submittedUpdates": 500,
                              "unsafeProcessedUpdates": 500,
                              "safeProcessedUpdates": 500,
                              "workers": 4,
                              "unsafeElapsedMs": 12,
                              "safeElapsedMs": 18,
                              "unsafeThroughputPerSec": 41666,
                              "safeThroughputPerSec": 27777,
                              "safeInvariantPassed": true,
                              "coins": []
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Gecersiz parametre",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Baska bir simulasyon zaten calisiyor",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Beklenmeyen sunucu hatasi",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    ResponseEntity<StatsResponse> simulate(
            @Parameter(description = "Uretilecek fiyat guncelleme gorevi sayisi (1-100000)", example = "500")
            @RequestParam @Min(1) @Max(100000) int updates,

            @Parameter(description = "Paralel calisacak worker thread sayisi (1-16)", example = "4")
            @RequestParam @Min(1) @Max(16) int workers,

            @Parameter(description = "Rastgele sayi uretici seed degeri", example = "42")
            @RequestParam(required = false) Long seed
    );

    @Operation(
            summary = "Coin listesini getir",
            description = "Son tamamlanan simulasyonun safe modundaki coin anlik goruntulerini doner."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Coin listesi basariyla getirildi",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CoinResponse.class),
                    examples = @ExampleObject(value = """
                            [
                              {
                                "id": "BTC",
                                "initialPrice": 60000,
                                "currentPrice": 60150,
                                "updateCount": 167,
                                "lastDelta": 5,
                                "lastUpdatedBy": "safe-worker-3"
                              }
                            ]
                            """)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tamamlanmis simulasyon bulunamadi",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Beklenmeyen sunucu hatasi",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    ResponseEntity<List<CoinResponse>> getCoins();

    @Operation(
            summary = "Istatistikleri getir",
            description = "Son tamamlanan simulasyonun ozet istatistiklerini doner."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Istatistikler basariyla getirildi",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatsResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "Tamamlanmis simulasyon bulunamadi",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Beklenmeyen sunucu hatasi",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    ResponseEntity<StatsResponse> getStats();
}
