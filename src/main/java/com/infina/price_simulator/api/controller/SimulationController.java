package com.infina.price_simulator.api.controller;

import com.infina.price_simulator.api.dto.CoinComparisonResponse;
import com.infina.price_simulator.api.dto.CoinResponse;
import com.infina.price_simulator.api.dto.ErrorResponse;
import com.infina.price_simulator.api.dto.StatsResponse;
import com.infina.price_simulator.model.CoinComparison;
import com.infina.price_simulator.model.CoinRunSnapshot;
import com.infina.price_simulator.model.SimulationStats;
import com.infina.price_simulator.service.SimulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@RestController
@Validated
@Tag(name = "simulation", description = "Simülasyon başlatma ve sonuç sorgulama işlemleri")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @Operation(
            summary = "Simülasyon başlat",
            description = """
                    Belirtilen sayıda fiyat güncelleme görevi üretir ve hem thread-unsafe hem de
                    thread-safe modda çalıştırır. Her iki modun sonuçlarını karşılaştırır ve
                    safe modun deterministik invariantı sağlayıp sağlamadığını doğrular.
                    
                    **Kısıtlamalar:**
                    - `updates`: 1 ile 100.000 arasında olmalıdır
                    - `workers`: 1 ile 16 arasında olmalıdır
                    - Aynı anda yalnızca bir simülasyon çalışabilir
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Simülasyon başarıyla tamamlandı",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StatsResponse.class),
                            examples = @ExampleObject(
                                    name = "Başarılı yanıt",
                                    value = """
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
                                              "coins": [
                                                {
                                                  "id": "BTC",
                                                  "initial": 60000,
                                                  "expected": 60150,
                                                  "unsafe": 59980,
                                                  "safe": 60150,
                                                  "expectedUpdateCount": 167,
                                                  "unsafeUpdateCount": 162,
                                                  "safeUpdateCount": 167
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Geçersiz parametre — updates veya workers kısıt dışı ya da eksik",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "updates sınır dışı",
                                            value = """
                                                    {
                                                      "timestamp": "2024-01-15T10:30:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "simulate.updates: must be greater than or equal to 1",
                                                      "path": "/simulate"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "workers sınır dışı",
                                            value = """
                                                    {
                                                      "timestamp": "2024-01-15T10:30:00",
                                                      "status": 400,
                                                      "error": "Bad Request",
                                                      "message": "simulate.workers: must be less than or equal to 16",
                                                      "path": "/simulate"
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Çakışma — Başka bir simülasyon zaten çalışıyor",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Simülasyon zaten çalışıyor",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 409,
                                              "error": "Conflict",
                                              "message": "A simulation is already running.",
                                              "path": "/simulate"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Sunucu İçi Hata — Beklenmeyen bir durum oluştu",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Sunucu Hatası",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred: ...",
                                              "path": "/simulate"
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/simulate")
    public ResponseEntity<StatsResponse> simulate(
            @Parameter(
                    description = "Üretilecek fiyat güncelleme görevi sayısı (1–100.000)",
                    required = true,
                    example = "500"
            )
            @RequestParam @Min(1) @Max(100000) int updates,

            @Parameter(
                    description = "Paralel çalışacak worker thread sayısı (1–16)",
                    required = true,
                    example = "4"
            )
            @RequestParam @Min(1) @Max(16) int workers,

            @Parameter(
                    description = "Rastgele sayı üretici tohum değeri. Verilmezse sistem zamanı kullanılır. " +
                            "Aynı seed ile sonuçlar tekrarlanabilir.",
                    example = "42"
            )
            @RequestParam(required = false) Long seed) {

        // Seed verilmemişse çalıştırma anında üretilir ve cevapta geri döner
        long effectiveSeed = seed != null ? seed : System.nanoTime();

        SimulationStats stats = simulationService.runSimulation(updates, workers, effectiveSeed);
        return ResponseEntity.ok(toStatsResponse(stats));
    }

    @Operation(
            summary = "Coin listesini getir",
            description = """
                    Son tamamlanan simülasyonun safe modundaki coin anlık görüntülerini döner.
                    Her coin için mevcut fiyat, güncelleme sayısı, son delta ve son güncelleyen
                    thread adı yer alır.
                    
                    **Not:** Hiç simülasyon tamamlanmamışsa 404 döner.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Coin listesi başarıyla getirildi",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CoinResponse.class),
                            examples = @ExampleObject(
                                    name = "Coin listesi",
                                    value = """
                                            [
                                              {
                                                "id": "BTC",
                                                "initialPrice": 60000,
                                                "currentPrice": 60150,
                                                "updateCount": 167,
                                                "lastDelta": 5,
                                                "lastUpdatedBy": "safe-worker-3"
                                              },
                                              {
                                                "id": "ETH",
                                                "initialPrice": 3000,
                                                "currentPrice": 2980,
                                                "updateCount": 166,
                                                "lastDelta": -10,
                                                "lastUpdatedBy": "safe-worker-1"
                                              },
                                              {
                                                "id": "SOL",
                                                "initialPrice": 150,
                                                "currentPrice": 153,
                                                "updateCount": 167,
                                                "lastDelta": 2,
                                                "lastUpdatedBy": "safe-worker-2"
                                              }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tamamlanmış simülasyon bulunamadı",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Simülasyon bulunamadı",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "No completed simulation result found.",
                                              "path": "/coins"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Sunucu İçi Hata — Beklenmeyen bir durum oluştu",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Sunucu Hatası",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred: ...",
                                              "path": "/coins"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/coins")
    public ResponseEntity<List<CoinResponse>> getCoins() {
        List<CoinRunSnapshot> coins = simulationService.getCoins();
        return ResponseEntity.ok(coins.stream().map(this::toCoinResponse).toList());
    }

    @Operation(
            summary = "İstatistikleri getir",
            description = """
                    Son tamamlanan simülasyonun özet istatistiklerini döner.
                    Unsafe ve safe modların performans karşılaştırması (süre, throughput) ile
                    invariant doğrulama sonucunu içerir.
                    
                    **Not:** Hiç simülasyon tamamlanmamışsa 404 döner.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "İstatistikler başarıyla getirildi",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StatsResponse.class),
                            examples = @ExampleObject(
                                    name = "İstatistik yanıtı",
                                    value = """
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
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Tamamlanmış simülasyon bulunamadı",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Simülasyon bulunamadı",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 404,
                                              "error": "Not Found",
                                              "message": "No completed simulation result found.",
                                              "path": "/stats"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Sunucu İçi Hata — Beklenmeyen bir durum oluştu",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Sunucu Hatası",
                                    value = """
                                            {
                                              "timestamp": "2024-01-15T10:30:00",
                                              "status": 500,
                                              "error": "Internal Server Error",
                                              "message": "An unexpected error occurred: ...",
                                              "path": "/stats"
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        SimulationStats stats = simulationService.getStats();
        return ResponseEntity.ok(toStatsResponse(stats));
    }

    private StatsResponse toStatsResponse(SimulationStats stats) {
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