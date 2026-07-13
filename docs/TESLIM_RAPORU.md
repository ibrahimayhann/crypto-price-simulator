# Teslim Raporu — Eşzamanlı Kripto Fiyat Simülatörü

> Bu dosya reponun kökünde `TESLIM_RAPORU.md` adıyla bulunmalıdır. Proje ilerledikçe gerçek linkler, metrikler ve kanıtlarla doldurulacaktır. README ile çelişen bilgi bırakılmamalıdır.

## 1. Grup Bilgileri

| Alan | Bilgi |
|---|---|
| Grup adı | `<BELİRLENECEK>` |
| Grup üyeleri (5) | `<Üye-1, Üye-2, Üye-3, Üye-4, Üye-5>` |
| GitHub repo linki | `<EKLENECEK>` |
| Pull Request linkleri | `<PR-1>, <PR-2>, <PR-3>, <PR-4>, <PR-5>, ...` |
| Conflict çözülen dosya/bölüm | `<README.md / Takım sloganı>` |
| Conflict branch'leri | `<docs/conflict-a>, <docs/conflict-b>` |
| Conflict çözüm commit / PR | `<EKLENECEK>` |
| Yapılan bonus | `Yok / Virtual Threads / CompletableFuture / Deadlock` |

## 2. Kısa Açıklama

Bu proje, seed ile üretilen aynı immutable fiyat güncelleme görevlerini expected, unsafe ve safe akışlarda çalıştıran bellek içi bir Spring Boot uygulamasıdır. Açık bir `BlockingQueue`, sabit worker havuzu, unsafe/safe sayaçlar ve coin başına kilitleme kullanılarak race condition gözlemlenir; güvenli sonucun doğruluğu invariant'larla kanıtlanır.

## 3. Çalıştırma

```bash
git clone <REPO_LINKI>
cd infina-concurrent-crypto-simulator
mvn clean test
mvn spring-boot:run
```

Windows Maven Wrapper varsa:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

- Swagger: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Örnek: `POST /simulate?updates=10000&workers=4&seed=42`
- Ardından: `GET /stats` ve `GET /coins`

## 4. Mimari Özeti

```text
Controller -> SimulationService -> TaskProducer
                              -> ExpectedResultCalculator
                              -> SimulationEngine(UNSAFE / SAFE)
SimulationEngine -> TaskQueue(BlockingQueue) -> Fixed Worker Pool
Worker -> Counter + CoinState
StatsCollector -> InvariantChecker -> Immutable SimulationStats
```

Mutable coin state controller'a doğrudan verilmez; lock altında oluşturulan immutable snapshot/response nesneleri kullanılır.

## 5. Tasarım Kararları

> Kodda farklı bir seçim yapılırsa bu tablo ve README aynı PR içinde güncellenmelidir.

| Karar noktası | Karar | Gerekçe |
|---|---|---|
| Görev kuyruğu | `ArrayBlockingQueue(Math.max(1, Math.min(updates, 1_000)))` | Sınırlı kapasite ve backpressure; sınırsız büyüme riskini azaltır. |
| Worker havuzu | `newFixedThreadPool(workers)` | Thread sayısını sınırlar, thread reuse sağlar, görev başına thread açmaz. |
| Güvenli sayaç | `AtomicLong` | Tek değişken üzerindeki artırma için sade atomik çözüm. |
| Coin kilidi | `ReentrantLock` | Dört state alanını tek kritik bölümde tutarlı günceller. |
| Lock kapsamı | Coin başına lock | Farklı coin'ler paralel ilerler; global lock gereksiz contention oluşturmaz. |
| İşlerin tamamlanması | `CountDownLatch` + poison pill | Latch gerçek görevleri bekler; poison pill queue'da bekleyen worker'ları durdurur. |
| Graceful shutdown | `shutdown/awaitTermination/shutdownNow` | Mevcut işlerin tamamlanmasını bekler, timeout ve interrupt durumunu yönetir. |
| Sonucun paylaşılması | Immutable stats + `AtomicReference` | Son tamamlanan sonuç yarım state olmadan güvenli yayımlanır. |
| İkinci simülasyon | `AtomicBoolean.compareAndSet` | Aynı anda tek simülasyon; `finally` ile her durumda serbest bırakma. |

## 6. Endpoint ve Hata Özeti

| Endpoint / durum | Beklenen sonuç |
|---|---|
| `POST /simulate?updates=10000&workers=4&seed=42` | İşler tamamlandıktan sonra sonuç |
| `GET /coins` | Son safe coin snapshot'ları |
| `GET /stats` | Son expected/unsafe/safe karşılaştırması ve metrikler |
| Geçersiz `updates/workers` | HTTP 400 |
| Sonuç yokken `/stats` | HTTP 404 |
| Simülasyon sürerken ikinci `/simulate` | HTTP 409 |

## 7. Race Condition Kanıtı

### Çalıştırma bilgisi

| Alan | Değer |
|---|---|
| Seed | `<...>` |
| Updates | `<...>` |
| Workers | `<...>` |
| Deneme sayısı | `<...>` |
| Unsafe'in bozulduğu deneme sayısı | `<...>` |

### Coin sonuçları

| Coin | Initial | Expected | Unsafe | Safe | Expected count | Unsafe count | Safe count |
|---|---:|---:|---:|---:|---:|---:|---:|
| BTC | 60.000 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| ETH | 3.000 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| SOL | 150 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |

### Toplam sayaç

```text
submittedUpdates:       <...>
unsafeProcessedUpdates: <...>
safeProcessedUpdates:   <...>
```

Gözlem:

```text
<Unsafe sonuç neden ve hangi şartlarda bozuldu? Worker/update sayısı arttığında ne oldu?>
```

## 8. Invariant Kanıtı

```text
safePrice       == initialPrice + sum(all deltas)        -> <PASSED/FAILED>
safeUpdateCount == o coin için üretilen görev sayısı     -> <PASSED/FAILED>
safeProcessed   == submittedUpdates                      -> <PASSED/FAILED>
```

`safeInvariantPassed: <true/false>`

## 9. Metrik Özeti

Benchmark sırasında görev bazlı DEBUG logları kapalı tutulmuştur: `<EVET/HAYIR>`

| Updates | Seed | Workers | Unsafe ms | Safe ms | Unsafe task/s | Safe task/s | Invariant |
|---:|---:|---:|---:|---:|---:|---:|---|
| 50.000 | `<...>` | 1 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| 50.000 | `<...>` | 2 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| 50.000 | `<...>` | 4 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| 50.000 | `<...>` | 8 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |

Performans yorumu:

```text
<Context switching, lock contention ve worker sayısının etkisi>
```

## 10. Thread Dump Özeti

| Kontrol | Gözlem |
|---|---|
| İstenen worker sayısı | `<...>` |
| Dump'ta görülen safe worker | `<...>` |
| Dump'ta görülen unsafe worker | `<...>` |
| `WAITING` worker'lar | `<BlockingQueue.take() gözlemi>` |
| `RUNNABLE` worker'lar | `<...>` |
| Lock contention | `<...>` |
| Java-level deadlock | `<Yok / Var ve açıklama>` |

Kısa dump kesiti:

```text
<worker thread kesiti>
```

Ayrıntılı dosya: `docs/thread-dump/<DOSYA_ADI>`

## 11. Test Özeti

Test komutu:

```bash
mvn clean verify
```

| Test | Durum | Sınıf/metot |
|---|---|---|
| Seed tekrarlanabilirliği | `<PASSED>` | `<...>` |
| Expected fiyat hesabı | `<PASSED>` | `<...>` |
| Safe counter concurrency | `<PASSED>` | `<...>` |
| Safe coin invariant/snapshot | `<PASSED>` | `<...>` |
| Validation → 400 | `<PASSED>` | `<...>` |
| Sonuç yok → 404 | `<PASSED>` | `<...>` |
| İkinci simülasyon → 409 | `<PASSED>` | `<...>` |
| Controller integration | `<PASSED>` | `<...>` |

Toplam test sonucu:

```text
Tests run: <...>, Failures: <...>, Errors: <...>, Skipped: <...>
```

## 12. Merge Conflict Deneyimi

| Alan | Bilgi |
|---|---|
| Branch 1 | `<docs/conflict-a>` |
| Branch 2 | `<docs/conflict-b>` |
| Dosya / bölüm | `<README.md / Takım sloganı>` |
| Branch 1 değişikliği | `<...>` |
| Branch 2 değişikliği | `<...>` |
| Korunan nihai içerik | `<...>` |
| Çözüm aracı | `<IntelliJ / terminal>` |
| Çözüm commit / PR | `<...>` |
| Öğrenilen | `<...>` |

## 13. GitHub ve Ekip Kanıtı

- Her üye en az bir feature branch açtı: `<EVET/HAYIR>`
- Her üye en az bir anlamlı PR oluşturdu: `<EVET/HAYIR>`
- Her PR başka bir üye tarafından incelendi: `<EVET/HAYIR>`
- Her üye en az bir code review yorumu yaptı: `<EVET/HAYIR>`
- Ana geliştirmeler doğrudan `main`e gönderilmedi: `<EVET/HAYIR>`
- CI zorunlu status check olarak kullanıldı: `<EVET/HAYIR>`

## 14. Bireysel Katkı Tablosu

| Üye | Rol / Ne yaptı? | Branch | Pull Request | Review ettiği PR |
|---|---|---|---|---|
| `<Üye-1>` | Coin & State | `feature/coin-state` | `<link>` | `<link>` |
| `<Üye-2>` | Producer & Worker Pool | `feature/producer-worker-pool` | `<link>` | `<link>` |
| `<Üye-3>` | Counter & Invariant | `feature/counter-invariant` | `<link>` | `<link>` |
| `<Üye-4>` | API & Swagger | `feature/simulation-api` | `<link>` | `<link>` |
| `<Üye-5>` | Engine, Service & Metrics | `feature/simulation-engine` | `<link>` | `<link>` |

## 15. Zorunlu Özellikler — Öz Değerlendirme

### Uygulama ve API

- [ ] Uygulama hatasız ayağa kalkıyor.
- [ ] `/simulate`, `/coins`, `/stats` çalışıyor.
- [ ] Geçersiz parametre → HTTP 400.
- [ ] Sonuç yok → HTTP 404.
- [ ] İkinci eşzamanlı istek → HTTP 409.
- [ ] Swagger çalışıyor ve README'de adres var.

### Concurrency

- [ ] Aynı immutable görev listesi expected/safe/unsafe'de kullanılıyor.
- [ ] Açık `BlockingQueue` + sabit thread pool var.
- [ ] Her görev için yeni thread açılmıyor.
- [ ] Unsafe sayaç ve coin state gerçek race condition gösteriyor.
- [ ] Safe sayaç `AtomicLong`.
- [ ] Safe coin state `ReentrantLock` ve coin başına lock kullanıyor.
- [ ] İşlerin bitmesi bekleniyor.
- [ ] Graceful shutdown uygulanıyor.
- [ ] Seed ile tekrarlanabilir görev üretimi var.
- [ ] Sonuç immutable ve güvenli yayımlanıyor.

### Doğruluk ve gözlem

- [ ] Safe invariant başarılı.
- [ ] Süre ve throughput kaydedildi.
- [ ] 1/2/4/8 worker tablosu dolduruldu.
- [ ] Thread dump alındı ve yorumlandı.

### Test ve GitHub

- [ ] Unit testler var.
- [ ] En az bir integration test var.
- [ ] Her üyenin branch, PR ve review katkısı var.
- [ ] En az bir merge conflict çözüldü.
- [ ] README ve teslim raporu eksiksiz.

## 16. Bonus Çalışmalar

| Bonus | Yapıldı mı? | Branch/PR | Sonuç |
|---|---|---|---|
| Virtual Threads karşılaştırması | `<Hayır/Evet>` | `<...>` | `<...>` |
| CompletableFuture | `<Hayır/Evet>` | `<...>` | `<...>` |
| Deadlock + lock ordering | `<Hayır/Evet>` | `<...>` | `<...>` |

## 17. Notlar

```text
<Zorlanılan noktalar, tartışılan alternatifler, gelecekte yapılabilecek iyileştirmeler>
```
