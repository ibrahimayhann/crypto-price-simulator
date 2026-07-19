package com.infina.price_simulator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI simulatorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kripto Fiyat Simülatörü API")
                        .description("""
                                ## Genel Bakış
                                Producer-Consumer mimarisine dayalı, çok iş parçacıklı kripto para fiyat güncelleme simülasyonu.
                                
                                ## Özellikler
                                - **Unsafe / Safe karşılaştırması**: Thread-unsafe `long` ile `ReentrantLock` korumalı state yan yana çalışır.
                                - **Invariant doğrulaması**: Safe akışın sonucu deterministik expected değerlerle karşılaştırılır.
                                - **Yapılandırılabilir yük**: `updates` (1-100 000) ve `workers` (1-16) parametreleriyle test yükü ayarlanır.
                                - **Tekrarlanabilirlik**: Aynı `seed` ile aynı sonuçlar elde edilir.
                                
                                ## Hata Kodları
                                | Kod | Açıklama |
                                |-----|----------|
                                | 400 | Geçersiz parametre (updates veya workers sınır dışı) |
                                | 404 | Henüz tamamlanmış simülasyon yok |
                                | 409 | Başka bir simülasyon zaten çalışıyor |
                                | 500 | Beklenmeyen sunucu hatası |
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Infina Development Team")
                                .email("dev@infina.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Yerel Geliştirme Sunucusu")
                ))
                .tags(List.of(
                        new Tag().name("simulation").description("Simülasyon başlatma ve sonuç sorgulama işlemleri")
                ));
    }
}
