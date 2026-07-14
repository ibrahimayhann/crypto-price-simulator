# Teslim Raporu — Eşzamanlı Kripto Fiyat Simülatörü

> Bu dosya final teslim kanıtıdır. Gerçek link, sayı veya test çıktısı
> bulunmadan ilgili madde tamamlandı olarak işaretlenmemelidir.

## 1. Grup Bilgileri

| Alan | Bilgi |
|---|---|
| Grup adı | `<FINALDE DOLDURULACAK>` |
| Grup üyeleri | İbrahim, Fırat, Ahmet, Cem Bora, Tolga |
| GitHub repository | `<FINALDE DOLDURULACAK>` |
| Final commit | `<FINALDE DOLDURULACAK>` |
| CI workflow | `<FINALDE DOLDURULACAK>` |
| Bonus | Yok / `<VARSA AÇIKLAYIN>` |

## 2. Proje Özeti

Bu proje, seed ile üretilen aynı immutable `PriceUpdateTask` listesini expected,
unsafe ve safe akışlarda çalıştıran in-memory bir Spring Boot uygulamasıdır.

Ana concurrency araçları:

- `ArrayBlockingQueue`,
- Fixed worker pool,
- Unsafe `long` counter,
- Safe `AtomicLong` counter,
- Coin başına `ReentrantLock`,
- `CountDownLatch`,
- Poison pill,
- Graceful shutdown.

Ana package:

```text
com.infina.price_simulator
```

Uygulama adı:

```text
price-simulator
```

## 3. Geliştirme Durumu

| Issue | Sorumlu | Durum / Kanıt |
|---|---|---|
| #1–#5 | İbrahim | Kod/test tamamlandı — PR linkleri aşağıya eklenecek |
| #6–#10 | Fırat | Kod/evidence altyapısı tamamlandı — gerçek çıktı ve PR linkleri eklenecek |
| #11–#15 | Ahmet | `<FINAL DURUM>` |
| #16–#20 | Ortak | `<FINAL DURUM>` |

## 4. Çalıştırma

```powershell
git clone <REPO_URL>
cd <REPO_KLASORU>
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/api-docs
```

Örnek akış:

```text
POST /simulate?updates=10000&workers=4&seed=42
GET /stats
GET /coins
```

## 5. Tasarım Kararları

| Karar | Uygulama | Gerekçe |
|---|---|---|
| Görev modeli | Immutable record | Thread'ler arası değişmez iş yükü |
| Görev üretimi | `Random(seed)` | Tekrarlanabilir deney |
| Görev listesi | `List.copyOf()` | Expected/unsafe/safe arasında değişikliği engellemek |
| Queue | Sınırlı `ArrayBlockingQueue` | Backpressure ve bellek kontrolü |
| Worker pool | Fixed thread pool | Thread sınırı ve reuse |
| Unsafe counter | `long` increment | Race condition gözlemi |
| Safe counter | `AtomicLong` | Atomik tek alan güncellemesi |
| Safe coin | Coin başına `ReentrantLock` | Çok alanlı state'in tutarlı güncellenmesi |
| Completion | `CountDownLatch` | Gerçek task'ların tamamını beklemek |
| Worker bitişi | Poison pill | Queue'da bekleyen worker'ları sonlandırmak |
| Shutdown | shutdown/await/shutdownNow | Timeout ve interrupt kontrollü kapanma |
| Sonuç yayını | `<AHMET'İN GERÇEK UYGULAMASI>` | Son completed result'ın güvenli paylaşılması |
| İkinci simülasyon | `<AHMET'İN GERÇEK UYGULAMASI>` | Aynı anda tek run |

## 6. Endpoint ve Hata Kanıtı

| Kontrol | Beklenen | Gerçek sonuç / test |
|---|---|---|
| `POST /simulate` | 200 | `<...>` |
| `GET /coins` | 200 / sonuç yoksa 404 | `<...>` |
| `GET /stats` | 200 / sonuç yoksa 404 | `<...>` |
| Geçersiz updates | 400 | `<...>` |
| Geçersiz workers | 400 | `<...>` |
| İkinci simülasyon | 409 | `<...>` |
| Beklenmeyen execution hatası | 500 | `<...>` |

## 7. Race Condition Kanıtı

Kaynak:

```text
docs/evidence/race-observation.md
```

| Alan | Değer |
|---|---|
| Seed | `<GERÇEK>` |
| Updates | `<GERÇEK>` |
| Workers | `<GERÇEK>` |
| Run sayısı | `<GERÇEK>` |
| Unsafe counter sapma sayısı | `<GERÇEK>` |
| Unsafe coin sapma sayısı | `<GERÇEK>` |
| Safe invariant başarısı | `<GERÇEK>` |

Unsafe sonuç yapay olarak değiştirilmemiştir: `<EVET/HAYIR>`

## 8. Coin Karşılaştırması

| Coin | Initial | Expected | Unsafe | Safe | Expected count | Unsafe count | Safe count |
|---|---:|---:|---:|---:|---:|---:|---:|
| BTC | 60.000 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| ETH | 3.000 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| SOL | 150 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |

## 9. Invariant Kanıtı

```text
safePrice == initialPrice + sum(coin deltas)     -> <PASSED/FAILED>
safeUpdateCount == generated coin task count     -> <PASSED/FAILED>
safeProcessedCount == submittedUpdates           -> <PASSED/FAILED>
```

## 10. Benchmark

Kaynak:

```text
docs/evidence/benchmark-results.md
```

Benchmark sırasında task DEBUG logları kapalı: `<EVET/HAYIR>`

| Updates | Seed | Workers | Unsafe ms | Safe ms | Unsafe task/s | Safe task/s | Invariant |
|---:|---:|---:|---:|---:|---:|---:|---|
| `<...>` | `<...>` | 1 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| `<...>` | `<...>` | 2 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| `<...>` | `<...>` | 4 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| `<...>` | `<...>` | 8 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |

Performans yorumu:

```text
<CPU çekirdeği, context switching, kısa task süresi ve lock contention yorumu>
```

## 11. Thread Dump

Kaynaklar:

```text
docs/evidence/thread-dump.txt
docs/evidence/thread-dump-analysis.md
```

| Kontrol | Gözlem |
|---|---|
| İstenen worker sayısı | `<...>` |
| Görülen safe worker | `<...>` |
| Görülen unsafe worker | `<...>` |
| Queue wait | `<...>` |
| Lock contention | `<...>` |
| Java-level deadlock | `<YOK/VAR>` |
| Run sonrası worker leak | `<YOK/VAR>` |

## 12. Test Özeti

Komut:

```powershell
.\mvnw.cmd clean verify
```

| Test alanı | Sınıf/metot | Durum |
|---|---|---|
| Seed repeatability | `<...>` | `<PASSED>` |
| Safe counter | `<...>` | `<PASSED>` |
| Safe coin state | `<...>` | `<PASSED>` |
| Expected calculator | `<...>` | `<PASSED>` |
| Invariant | `<...>` | `<PASSED>` |
| Queue blocking/backpressure | `<...>` | `<PASSED>` |
| Worker/engine/shutdown | `<...>` | `<PASSED>` |
| Validation 400 | `<...>` | `<PASSED>` |
| Result yok 404 | `<...>` | `<PASSED>` |
| Concurrent request 409 | `<...>` | `<PASSED>` |
| Controller integration | `<...>` | `<PASSED>` |

Maven özeti:

```text
Tests run: <...>, Failures: <...>, Errors: <...>, Skipped: <...>
```

## 13. Pull Request ve Review Kanıtları

| Üye | Issue'lar | Branch / PR | Review ettiği PR |
|---|---|---|---|
| İbrahim | #1–#5 | `<LINKLER>` | `<LINK>` |
| Fırat | #6–#10 | `<LINKLER>` | `<LINK>` |
| Ahmet | #11–#15 | `<LINKLER>` | `<LINK>` |
| Cem Bora | Ar-Ge/docs | `<LINK>` | `<LINKLER>` |
| Tolga | Ar-Ge/docs | `<LINK>` | `<LINKLER>` |

## 14. Merge Conflict

| Alan | Bilgi |
|---|---|
| Branch 1 | `<...>` |
| Branch 2 | `<...>` |
| Dosya ve satır | `<...>` |
| Conflict çözüm aracı | `<IntelliJ/terminal>` |
| Korunan içerik | `<...>` |
| Commit / PR | `<LINK>` |
| Öğrenilenler | `<...>` |

## 15. Final Öz Değerlendirme

### Uygulama/API

- [ ] Uygulama hatasız başlıyor.
- [ ] `/simulate`, `/coins`, `/stats` çalışıyor.
- [ ] 400, 404, 409 doğru.
- [ ] Swagger çalışıyor.

### Concurrency

- [ ] Task'lar immutable.
- [ ] Aynı liste expected/unsafe/safe'de kullanılıyor.
- [ ] Açık BlockingQueue var.
- [ ] Fixed pool var.
- [ ] Her task için thread yok.
- [ ] Unsafe gerçek race davranışı gösteriyor.
- [ ] Safe counter AtomicLong.
- [ ] Safe coin ReentrantLock.
- [ ] Completion bekleniyor.
- [ ] Graceful shutdown doğru.
- [ ] Sonuç güvenli yayımlanıyor.

### Kanıt

- [ ] Safe invariant başarılı.
- [ ] 1/2/4/8 benchmark gerçek değerlerle dolu.
- [ ] Race observation gerçek değerlerle dolu.
- [ ] Thread dump alınmış ve yorumlanmış.
- [ ] Sahte/örnek metrik kalmamış.

### GitHub ve doküman

- [ ] Her üyenin branch/PR/review katkısı var.
- [ ] Merge conflict çözülmüş.
- [ ] README ile rapor çelişmiyor.
- [ ] Placeholder kalmamış.
- [ ] Clean clone testi başarılı.
- [ ] Final CI başarılı.

## 16. Final Onay

| Rol | İsim | Karar | Tarih |
|---|---|---|---|
| Ar-Ge reviewer | Cem Bora | `<APPROVED/CHANGES REQUESTED>` | `<...>` |
| Ar-Ge reviewer | Tolga | `<APPROVED/CHANGES REQUESTED>` | `<...>` |
