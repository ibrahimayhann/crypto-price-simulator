<div align="center">
  <h1>Kripto Fiyat Simülatörü (Crypto Price Simulator)</h1>
  <p>
    <strong>Java eşzamanlılık mekanizmalarını göstermek üzere geliştirilmiş, bellek içi çalışan kripto para fiyat simülasyon motoru.</strong>
  </p>
  <p>
    <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java 21" />
    <img src="https://img.shields.io/badge/Spring_Boot-4.1.0-brightgreen.svg" alt="Spring Boot 4.1.0" />
    <img src="https://img.shields.io/badge/Concurrency-Virtual_Threads-blue.svg" alt="Virtual Threads" />
    <img src="https://img.shields.io/badge/Build-Maven-blueviolet.svg" alt="Maven" />
  </p>
</div>

---

## İçindekiler

- [Proje Hakkında](#proje-hakkında)
- [Kullanılan Teknolojiler](#kullanılan-teknolojiler)
- [Uygulamayı Çalıştırma](#uygulamayı-çalıştırma)
- [Swagger Adresi](#swagger-adresi)
- [Endpoint'ler](#endpointler)
- [Mimari Akış](#mimari-akış)
- [Tasarım Kararları](#tasarım-kararları)
- [Race Condition Gözlemi](#race-condition-gözlemi)
- [Güvenli Çözüm](#güvenli-çözüm)
- [Invariant ve Doğruluk Kanıtı](#invariant-ve-doğruluk-kanıtı)
- [Performans Sonuçları](#performans-sonuçları)
- [ReentrantLock ve synchronized Karşılaştırması](#reentrantlock-ve-synchronized-karşılaştırması)
- [Thread Dump İncelemesi](#thread-dump-incelemesi)
- [Merge Conflict Deneyimi](#merge-conflict-deneyimi)
- [Testler](#testler)
- [Grup Üyeleri ve Katkıları](#grup-üyeleri-ve-katkıları)
- [Bonus Çalışmalar](#bonus-çalışmalar)

## Proje Hakkında

Deterministik kripto para fiyat güncelleme görevlerini beklenen, güvensiz ve güvenli çalıştırma akışlarında işleyen bir Spring Boot projesidir. Concurrency hatalarının gözlemlenebilmesi ve güvenli sonucun invariant kontrolleriyle doğrulanabilmesi için üç akışta da aynı immutable görev listesi kullanılır.

Simülatör, başlangıçta bellekte tutulan üç coin ile çalışır:

| Coin | Başlangıç fiyatı |
|---|---:|
| BTC | 60.000 |
| ETH | 3.000 |
| SOL | 150 |

## Kullanılan Teknolojiler

| Teknoloji | Sürüm veya kullanım alanı |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.0 |
| Maven | Build ve dependency yönetimi |
| Spring Web MVC | REST API |
| Bean Validation | İstek parametrelerinin doğrulanması |
| Springdoc OpenAPI | 3.0.3 |
| JUnit 5 | Unit ve integration testleri |
| Java concurrency API’leri | `ExecutorService`, `BlockingQueue`, atomic türler ve lock’lar |
| Java 21 Virtual Threads | İsteğe bağlı bonus benchmark karşılaştırması |

## Uygulamayı Çalıştırma

### Gereksinimler

- Java 21
- Maven

### Projeyi klonlama ve doğrulama

```powershell
git clone https://github.com/ibrahimayhann/crypto-price-simulator.git
cd crypto-price-simulator
mvn.cmd clean verify
```

### Uygulamayı çalıştırma

```powershell
mvn.cmd spring-boot:run
```

Uygulama `http://localhost:8080` adresinde başlar.

## Swagger Adresi

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Yapılandırılmış yönlendirme adresi: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

Swagger index ve OpenAPI JSON endpoint’leri çalışan uygulama üzerinde doğrulanmıştır.

## Endpoint'ler

| Metot | Endpoint | Açıklama |
|---|---|---|
| `POST` | `/simulate` | Beklenen, güvensiz ve güvenli akışları çalıştırır ve sonuçların karşılaştırmasını döndürür. |
| `GET` | `/coins` | Son tamamlanan simülasyondaki güvenli coin snapshot’larını döndürür. |
| `GET` | `/stats` | Son tamamlanan simülasyonun istatistiklerini döndürür. |

### Simülasyon parametreleri

| Parametre | Zorunlu | Sınır | Açıklama |
|---|---|---|---|
| `updates` | Evet | `1..100000` | Immutable güncelleme görevi sayısı. |
| `workers` | Evet | `1..16` | Sabit worker pool boyutu. |
| `seed` | Hayır | `long` | Aynı görev listesinin yeniden üretilmesini sağlar. Verilmezse üretilen seed cevapta döndürülür. |

Örnek:

```powershell
curl.exe -X POST "http://localhost:8080/simulate?updates=10000&workers=4&seed=42"
```

### HTTP durum kodları

| Durum | Açıklama |
|---:|---|
| `200` | Başarılı simülasyon veya sonuç sorgusu. |
| `400` | Eksik veya izin verilen aralığın dışındaki istek parametresi. |
| `404` | `/coins` veya `/stats` için henüz tamamlanmış simülasyon bulunmaması. |
| `409` | Başka bir simülasyonun hâlihazırda çalışıyor olması. |
| `500` | Beklenmeyen çalıştırma hatası. |

## Mimari Akış

```text
POST /simulate
      |
      v
SimulationService
      |
      +--> TaskProducer --> tek immutable PriceUpdateTask listesi
      |
      +--> ExpectedCalculator (tek thread)
      |
      +--> SimulationEngine (UNSAFE)
      |
      +--> SimulationEngine (SAFE)
      |
      +--> invariant kontrolü --> immutable sonuç snapshot’ları
```

Engine ve service, coin kimliklerini ve başlangıç fiyatlarını ortak immutable `CoinCatalog` üzerinden okur. Normal platform thread akışındaki her engine çalıştırması kendi coin state’lerini, sayacını, bounded queue’sunu, completion latch’ini ve fixed worker pool’unu oluşturur:

```text
Producer --> ArrayBlockingQueue --> fixed PriceWorker pool --> coin state + counter
```

Kuyruk kapasitesi `min(görev sayısı, 1000)` olarak hesaplanır. Worker’lar, worker başına bir poison pill ile durdurulur. Tamamlanan istatistikler ve güvenli snapshot’lar `AtomicReference` üzerinden yayımlanır; `AtomicBoolean` aynı anda yalnızca bir simülasyonun çalışmasına izin verir.

İsteğe bağlı Virtual Thread benchmark akışı farklı çalışır:

```text
Immutable görev listesi --> görev başına bir Virtual Thread --> ortak TaskProcessor --> güvenli coin state + sayaç
```

Bonus akışı queue ve poison pill kullanmaz. Platform thread ve Virtual Thread akışları görev işleme, hata yakalama ve latch tamamlama mantığı için aynı `TaskProcessor` sınıfını kullanır.

### Concurrency Kavramları

- Fixed thread pool, her görev için ayrı thread oluşturmak yerine sınırlı sayıdaki worker thread’i yeniden kullanır. Bu yaklaşım thread oluşturma maliyetini, thread başına bellek kullanımını, scheduling baskısını ve context switching’i sınırlar.
- `ArrayBlockingQueue.put()`, kuyruk dolduğunda backpressure uygular. `take()`, boşta olan worker’ların busy waiting yapmadan beklemesini sağlar.
- Güvensiz sayaç atomik olmayan bir read-modify-write artırımı kullanır; bu nedenle eşzamanlı artırımlar kaybolabilir.
- Bir coin güncellemesi `currentPrice`, `updateCount`, `lastDelta` ve `lastUpdatedBy` alanlarını tek mantıksal işlem olarak değiştirir. Yalnızca tek bir alanın atomik olması bütün state’i tutarlı hâle getirmez.
- `CountDownLatch` bütün gerçek görevlerin tamamlanmasını bekler. Poison pill’ler queue consumer’larını durdurur ve sonuç snapshot’ları oluşturulmadan önce executor’ın sonlanması beklenir.
- Doğruluk, zamanlama veya log çıktısından tahmin edilmez; üretilen görev listesinden hesaplanarak kontrol edilir.

## Tasarım Kararları

| Karar | Mevcut uygulama | Ekip gerekçesi |
|---|---|---|
| Görev modeli | Immutable Java `record` | Worker’lar arasında paylaşılan görev verisinin sonradan değiştirilmesini engeller. |
| İş yükünün tekrarlanabilirliği | `Random(seed)` | Aynı seed ile aynı görev listesinin üretilmesini ve sonuçların karşılaştırılmasını sağlar. |
| Ortak iş yükü | Tek `List.copyOf()` görev listesi | Expected, unsafe ve safe akışların aynı veriyi işlemesini garanti eder. |
| Coin tanımları | Ortak immutable `CoinCatalog` | Coin kimliklerini ve başlangıç fiyatlarını engine ile service için tek kaynakta tutar. |
| Queue | Bounded `ArrayBlockingQueue` | Kuyruk dolduğunda backpressure uygular ve kuyruk belleğinin sınırsız büyümesini engeller. |
| Queue kapasitesi | `min(görev sayısı, 1000)` | Küçük iş yüklerinde gereksiz kapasite ayırmaz, büyük iş yüklerinde kuyruğu sınırlar. |
| Worker çalıştırma | Fixed thread pool | Her görev için yeni platform thread oluşturmak yerine sınırlı sayıdaki worker’ı yeniden kullanır. |
| Güvenli sayaç | `AtomicLong` | Tek sayaç değerini kaba bir global lock kullanmadan atomik olarak artırır. |
| Güvenli coin state | Coin başına bir `ReentrantLock` | Bir coinin çok alanlı güncellemesini birlikte korurken farklı coinlerin paralel güncellenmesine izin verir. |
| Completion | `CountDownLatch` | Koordinatör thread’in bütün gerçek görevler tamamlanana kadar beklemesini sağlar. |
| Worker sonlandırma | Poison pill | Queue üzerinde bekleyen worker’ların kontrollü şekilde döngüden çıkmasını sağlar. |
| Executor kapatma | `shutdown`, süre sınırlı `awaitTermination`, gerekirse `shutdownNow` | Önce kontrollü kapanmayı dener, süre aşılırsa zorunlu kapatma uygular. |
| Aktif simülasyon kontrolü | `AtomicBoolean` | `compareAndSet` ile aynı anda ikinci simülasyonu reddederek HTTP 409 üretilmesini sağlar. |
| Sonuç yayını | Immutable snapshot’larla `AtomicReference` | Tamamlanmış sonucu mutable engine state’ini REST katmanına açmadan güvenli biçimde yayımlar. |
| Bonus çalıştırma | Görev başına bir Virtual Thread | CPU-bound ve bekleme içeren iş yüklerini fixed platform thread havuzuyla karşılaştırmayı sağlar. |
| Fiyat Alt Sınırı | `Math.max(0, currentPrice + delta)` | Kripto para fiyatlarının sıfırın altına (negatif değerlere) düşerek mantıksız durumlar oluşturmasını engeller. |

## Race Condition Gözlemi

Kaydedilen stress çalışması `42` seed, `50.000` update, `4` worker ve `5` run kullanmıştır.

| Gözlem | Sonuç |
|---|---:|
| Unsafe counter sapması görülen run | 5 / 5 |
| Unsafe coin sapması görülen run | 5 / 5 |
| Safe invariant’ın başarılı olduğu run | 5 / 5 |
| Unsafe sonuç yapay olarak değiştirildi | Hayır |

Ayrıntılı run verileri ve analiz: [`docs/evidence/race-observation.md`](docs/evidence/race-observation.md)

## Güvenli Çözüm

| Konu | Güvensiz akış | Güvenli akış |
|---|---|---|
| İşlenen görev sayacı | Normal `long` artırımı | `AtomicLong.incrementAndGet()` |
| Coin güncellemesi | Korumasız compound update | Coin başına `ReentrantLock` |
| Beklenen sonuç | Uygulanmaz | Aynı görev listesinden sıralı olarak hesaplanır |
| Sonuç doğrulaması | Race condition nedeniyle sapabilir | Fiyat, güncelleme sayısı ve işlenen görev sayısı invariant’larıyla kontrol edilir |

Güvensiz sonucun her çalıştırmada hatalı olması beklenmez. Race condition’lar thread scheduling’e bağlıdır; uygulama güvensiz sonuçları yapay olarak değiştirmez.

## Invariant ve Doğruluk Kanıtı

Beklenen değerler, iki engine modu tarafından da kullanılan aynı immutable görev listesinden tek thread ile hesaplanır.

```text
safePrice = initialPrice + coin için üretilen bütün delta değerlerinin toplamı
safeUpdateCount = coin için üretilen görev sayısı
safeProcessedCount = submittedUpdates
```

Simülasyon yalnızca üç koşul da sağlandığında `safeInvariantPassed=true` döndürür.

## Performans Sonuçları

Kaydedilen iş yükü: `42` seed, `50.000` update ve kapalı task-level DEBUG logları.

| Workers | Unsafe ms | Safe ms | Unsafe task/s | Safe task/s | Safe invariant |
|---:|---:|---:|---:|---:|---|
| 1 | 37 | 21 | 1.351.351 | 2.380.952 | Başarılı |
| 2 | 17 | 15 | 2.941.176 | 3.225.806 | Başarılı |
| 4 | 9 | 8 | 5.555.556 | 6.549.252 | Başarılı |
| 8 | 11 | 12 | 4.545.455 | 4.094.481 | Başarılı |

Worker sayısı 1’den 4’e çıkarken süre azalmış ve throughput yükselmiştir. Worker sayısı 8’e çıktığında ise yalnızca üç coin kilidi üzerinde çalışan kısa görevlerde lock contention ve context switching maliyeti kazanımı azaltmıştır. Bu nedenle worker sayısını artırmak her zaman doğrusal performans artışı sağlamaz.

Ortam ve sonuç ayrıntıları: [`docs/evidence/benchmark-results.md`](docs/evidence/benchmark-results.md)

## ReentrantLock ve synchronized Karşılaştırması

| Konu | `ReentrantLock` | `synchronized` | Projedeki değerlendirme |
|---|---|---|---|
| Lock sahipliği | Açık `lock()` / `unlock()` çağrıları | Intrinsic monitor | Seçilen lock nesnesi `SafeCoinState` içinde açıkça görülür. |
| Serbest bırakma disiplini | `finally` içinde `unlock()` | Monitor kapsamından çıkıldığında otomatik bırakılır | Uygulama, hata durumunda da lock’ın bırakılması için `finally` kullanır. |
| Opsiyonel lock API’leri | `tryLock()` ve interruptible acquisition gibi API’leri destekler | Monitor girişini kullanır | Mevcut akış `lock()` kullansa da daha esnek lock API’leri kullanılabilir. |
| Projedeki kullanım | Her `SafeCoinState` için bir lock | Güvenli coin güncellemelerinde kullanılmıyor | Projede açık coin başına kilitleme için `ReentrantLock` seçilmiştir. |
| Seçim gerekçesi | Çok alanlı coin güncellemesini tek kritik bölümde korur | Mutual exclusion sağlayabilir | Projenin güvenli çözümü açık lock yönetimi üzerine kurulmuştur. |
| Coin başına lock ve global lock | BTC, ETH ve SOL için bağımsız lock | Tek ortak monitor bütün coinleri sıraya sokabilir | Coin başına lock, farklı coinler arasındaki paralelliği korur. |

<a id="thread-dump-incelemesi"></a>

## Thread Dump İncelemesi

Evidence dump, dört isimlendirilmiş safe worker queue’dan görev beklerken alınmıştır.

```text
"safe-worker-1" ... WAITING (parking)
    at java.util.concurrent.ArrayBlockingQueue.take(...)
```

| Kontrol | Gözlem |
|---|---|
| İstenen worker | 4 |
| Görülen safe worker | `safe-worker-1` ile `safe-worker-4` |
| Worker state | 4 / 4 `WAITING (parking)` |
| Queue wait | Dört worker’ın tamamında `ArrayBlockingQueue.take()` görüldü |
| `BLOCKED` worker | 0 |
| Java-level deadlock işareti | Bulunmadı |
| Kontrolsüz worker üretimi | Gözlemlenmedi |

- Ham dump: [`docs/evidence/thread-dump.txt`](docs/evidence/thread-dump.txt)
- Ayrıntılı analiz: [`docs/evidence/thread-dump-analysis.md`](docs/evidence/thread-dump-analysis.md)

## Merge Conflict Deneyimi

Doğrulanan Git geçmişi:

| Alan | Değer |
|---|---|
| Birinci branch | `conflict/readme-memory-a` |
| İkinci branch | `conflict/readme-memory-b` |
| Conflict çıkan dosya | `README.md` |
| İlk merge edilen PR | [#33](https://github.com/ibrahimayhann/crypto-price-simulator/pull/33) |
| Conflict çözüm PR’ı | [#32](https://github.com/ibrahimayhann/crypto-price-simulator/pull/32) |
| Conflict çözüm merge commit’i | `75f3037` |

## Testler

Varsayılan doğrulama paketini çalıştırmak için:

```powershell
mvn.cmd clean verify
```

Java 21 ile doğrulanan son sonuç:

```text
Tests run: 54, Failures: 0, Errors: 0, Skipped: 4
BUILD SUCCESS
```

Atlanan dört test; bir race observation testi, iki benchmark testi ve bir thread dump testinden oluşan isteğe bağlı evidence metotlarıdır:

```powershell
mvn.cmd "-DraceObservation=true" "-Dtest=RaceObservationEvidenceTest" test
mvn.cmd "-Dbenchmark=true" "-Dtest=SimulationBenchmarkTest" test
mvn.cmd "-DthreadDumpDemo=true" "-Dtest=ThreadDumpEvidenceTest" test
```

Test paketi deterministik görev üretimini, immutable görev listelerini, queue davranışını ve backpressure’ı, safe/unsafe sayaç ve state’leri, beklenen değer hesabını, invariant’ları, worker isimlendirmesini, completion’ı, shutdown’ı, validation’ı, controller integration’ını ve isteğe bağlı Virtual Thread karşılaştırmasını kapsar.

## Grup Üyeleri ve Katkıları

| Üye | Alan |
|---|---|
| İbrahim AYHAN | Core State & Testler |
| Fırat ASI | Engine, Queue, Benchmark |
| Ahmet KÖRELİ | API & Controller |
| Cem BORA | Ar-Ge, Dokümantasyon |
| Tolga | Mimari, Teslim Raporları |

## Bonus Çalışmalar

### Virtual Threads (Bonus A)

Bonus akışı `SimulationEngine` içinde isimlendirilmiş bir Virtual Thread factory ve `Executors.newThreadPerTaskExecutor(...)` kullanılarak uygulanmıştır. Her update için bir Virtual Thread gönderilir; platform thread akışıyla aynı `TaskProcessor` ve güvenli state uygulaması kullanılır.

Normal REST simülasyon akışı bounded queue ve fixed platform thread pool kullanmaya devam eder. Virtual Thread seçeneği şu anda HTTP parametresi olarak sunulmaz; isteğe bağlı benchmark testi üzerinden çalıştırılır.

Karşılaştırmayı çalıştırmak için:

```powershell
mvn.cmd "-Dbenchmark=true" "-Dtest=SimulationBenchmarkTest" test
```

19 Temmuz 2026 tarihinde yerel olarak doğrulanan sonuçlar:

| İş yükü | Mod | Süre (ms) | Throughput (task/s) |
|---|---|---:|---:|
| CPU-bound, 50.000 update | Platform, fixed 4 worker | 10 | 4.853.992 |
| CPU-bound, 50.000 update | Görev başına Virtual Thread | 122 | 409.080 |
| Simüle I/O, 1 ms beklemeli 1.000 update | Platform, fixed 4 worker | 505 | 1.978 |
| Simüle I/O, 1 ms beklemeli 1.000 update | Görev başına Virtual Thread | 14 | 68.032 |

Bu değerler çalışma ortamına bağlı tek çalıştırmalık ölçümlerdir. Bu çalıştırmada görev başına Virtual Thread, kısa CPU-bound iş yükünde ek maliyet oluşturmuş; görevler bekleme yaptığında ise fixed platform thread havuzundan belirgin biçimde daha iyi sonuç vermiştir.
