package com.infina.price_simulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI simulatorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Eşzamanlı Kripto Fiyat Simülatörü API")
                        .description("Producer-Consumer tabanlı fiyat güncelleme simülasyonu; " +
                                "güvenli/güvensiz akış karşılaştırması ve invariant doğrulaması sağlar.")
                        .version("v0.0.1"));
    }
}
