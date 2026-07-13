# Eşzamanlı Kripto Fiyat Simülatörü

> **İnfina Akademi · Java / Spring Boot / Concurrency Grup Ödevi**  
> Bu repository başlangıçta yalnızca proje iskeleti, ekip planı, GitHub kuralları ve teslim dokümantasyonunu içerir. Uygulama kodu feature branch'lerde, Pull Request ve review süreciyle geliştirilecektir.

## 1. Ödev Bilgileri

| Alan | Bilgi |
|---|---|
| Proje | Eşzamanlı Kripto Fiyat Simülatörü |
| İngilizce ad | Concurrent Crypto Price Simulator |
| Süre | 1 hafta |
| Teslim | 19 Temmuz 2026 Pazar, 23:59 |
| Grup | 5 geliştirici |
| Teslim şekli | GitHub repository linki + Pull Request linkleri |
| Zorunlu teknolojiler | Java 17+, Spring Boot, Maven, Git/GitHub, Swagger/OpenAPI |
| Seçilen teknoloji | Java 21, Spring Boot, Maven, Spring Web, Validation, Swagger/OpenAPI, JUnit 5 |
| Puan | 100 + en fazla 10 bonus |

## 2. Proje Hakkında

Bu proje kapsamlı bir kripto para uygulaması değildir. Domain bilinçli olarak sade tutulmuştur. Amaç; aynı immutable fiyat güncelleme görevlerini tek thread, güvensiz concurrency ve güvenli concurrency akışlarında çalıştırmak, race condition/lost update problemini gözlemlemek ve güvenli çözümün doğruluğunu invariant'larla kanıtlamaktır.

Uygulama:

- Çok sayıda görevi sınırlı sayıda worker ile işler.
- Kod üzerinde açıkça görünen bir `BlockingQueue` ile Producer–Consumer yapısı kurar.
- Unsafe ve safe sayaç/coin state davranışlarını karşılaştırır.
- Aynı görev listesini expected, unsafe ve safe çalışmalarında kullanır.
- Bütün işler bitmeden `POST /simulate` cevabı dönmez.
- Executor kaynaklarını graceful shutdown ile kapatır.
- Süre, throughput, invariant ve thread dump çıktılarıyla davranışı raporlar.
- GitHub branch, issue, PR, review ve merge conflict sürecini ekipçe görünür hâle getirir.

## 3. Kapsam Dışı

Bu ödevde aşağıdakiler kullanılmayacaktır:

- PostgreSQL, Hibernate/JPA veya başka bir veritabanı
- Kafka, Redis veya mikroservis mimarisi
- JWT/Spring Security
- Gerçek kripto borsası entegrasyonu
- Gerçek para veya ondalık fiyat hassasiyeti

Coin durumları ve son tamamlanan simülasyon sonucu uygulama belleğinde tutulacaktır. Yönerge gereği fiyat ve delta değerleri `double` yerine `long` olacaktır.

## 4. Spring Initializr Ayarları

| Alan | Değer |
|---|---|
| Project | Maven |
| Language | Java |
| Group | `com.infina.academy` |
| Artifact | `concurrent-crypto-simulator` |
| Package name | `com.infina.academy.pricesim` |
| Packaging | Jar |
| Java | 21 |
| Dependencies | Spring Web, Validation |

Initializr sonrasında şu bağımlılıklar eklenir:

- `springdoc-openapi-starter-webmvc-ui`
- `spring-boot-starter-test`

Bu repository `application.properties` kullanır; aynı ayarların tekrarlandığı bir `application.yml` eklenmemelidir.

## 5. Başlangıç Coin'leri ve Görev Modeli

| Coin | Başlangıç fiyatı |
|---|---:|
| BTC | 60.000 |
| ETH | 3.000 |
| SOL | 150 |

Her fiyat güncelleme görevi immutable bir `record` olmalıdır:

```java
public record PriceUpdateTask(
        long sequence,
        String coinId,
        long delta
) {
}
```

Örnek görevler:

```text
1 -> BTC +120
2 -> ETH -25
3 -> SOL +3
4 -> BTC -50
```

Görevler yalnızca bir kez üretilir. Aynı immutable liste şu üç işlemde kullanılır:

1. Tek thread ile expected sonucun hesaplanması
2. Unsafe simülasyon
3. Safe simülasyon

`seed` verilirse aynı parametrelerle aynı görev listesi üretilebilmelidir. `seed` verilmezse uygulama bir seed üretip sonuçta göstermelidir.

## 6. Nihai Mimari Akış

```text
POST /simulate
      |
      v
SimulationController
      |
      v
SimulationService
      |
      +--> AtomicBoolean: aynı anda yalnız bir simülasyon
      |
      +--> TaskProducer(seed) --> immutable List<PriceUpdateTask>
      |                              |
      |                              +--> ExpectedResultCalculator (tek thread)
      |                              +--> SimulationEngine(UNSAFE)
      |                              `--> SimulationEngine(SAFE)
      |
      v
TaskQueue --> BlockingQueue<PriceUpdateTask>
      |
      v
Fixed Worker Pool (workers parametresi)
      |
      +--> Unsafe/Safe ProcessedCounter
      `--> Unsafe/Safe CoinState
      |
      v
StatsCollector + InvariantChecker
      |
      v
Immutable SimulationStats
      |
      v
AtomicReference ile sonucun güvenli yayımlanması
```

Önemli ayrım:

- `BlockingQueue`, görevlerin producer ile worker'lar arasında güvenli aktarılmasını sağlar.
- Queue'nun thread-safe olması, queue'dan alınan mutable coin nesnesini otomatik olarak thread-safe yapmaz.
- Coin state ayrıca kendi lock stratejisiyle korunmalıdır.

## 7. Nihai Paket ve Sınıf Yapısı

Arkadaş yapısındaki açık `controller/dto/exception`, `TaskQueue`, `SimulationService`, `WorkerThreadFactory` ve `StatsCollector` ayrımları korunmuştur. Çakışmayı önlemek ve zorunlu çıktıları tamamlamak için immutable snapshot/result modelleri, eksik hata türleri ve test yapısı eklenmiştir.

```text
src/main/java/com/infina/academy/pricesim
|
|-- PriceSimApplication.java
|
|-- api
|   |-- controller
|   |   `-- SimulationController.java
|   |-- dto
|   |   |-- CoinResponse.java
|   |   |-- CoinComparisonResponse.java
|   |   |-- SimulationResponse.java
|   |   |-- StatsResponse.java
|   |   `-- ErrorResponse.java
|   `-- exception
|       |-- GlobalExceptionHandler.java
|       |-- SimulationAlreadyRunningException.java
|       |-- SimulationNotFoundException.java
|       `-- SimulationExecutionException.java
|
|-- config
|   `-- OpenApiConfig.java
|
|-- counter
|   |-- ProcessedCounter.java
|   |-- SafeCounter.java
|   `-- UnsafeCounter.java
|
|-- engine
|   |-- PriceWorker.java
|   |-- SimulationEngine.java
|   |-- SimulationMode.java
|   |-- TaskProducer.java
|   `-- TaskQueue.java
|
|-- metrics
|   |-- ExpectedResultCalculator.java
|   |-- InvariantChecker.java
|   |-- SimulationStats.java
|   `-- StatsCollector.java
|
|-- model
|   |-- CoinComparison.java
|   |-- CoinSnapshot.java
|   |-- ExpectedCoinResult.java
|   |-- PriceUpdateTask.java
|   `-- SimulationRunResult.java
|
|-- service
|   `-- SimulationService.java
|
|-- state
|   |-- CoinState.java
|   |-- SafeCoinState.java
|   `-- UnsafeCoinState.java
|
`-- util
    `-- WorkerThreadFactory.java
```

### 7.1 Neden `model/CoinState` kullanmıyoruz?

Arkadaş yapısında `model/CoinState` ile `state/SafeCoinState` ve `state/UnsafeCoinState` birlikte bulunuyor. Sorumluluklar net tanımlanmazsa aynı kavram üç farklı sınıfta dağılabilir. Nihai planda:

- `state/CoinState`: iki implementasyonun ortak davranış sözleşmesi
- `state/SafeCoinState`: coin başına `ReentrantLock` ile güvenli güncelleme
- `state/UnsafeCoinState`: bilinçli olarak kilitsiz güncelleme
- `model/CoinSnapshot`: dışarı verilen immutable ve tutarlı görüntü

kullanılır. Controller mutable state nesnesini doğrudan dışarı açmaz.

### 7.2 Sınıf Sorumlulukları

| Sınıf | Sorumluluk |
|---|---|
| `SimulationController` | `/simulate`, `/coins`, `/stats`; parametreleri alır ve service çağırır. |
| `SimulationService` | Seed, görev listesi, expected/unsafe/safe akış, latest result ve 409 kontrolünü orkestre eder. |
| `TaskProducer` | Seed ile tekrarlanabilir immutable görev listesi üretir. |
| `TaskQueue` | Seçilen `BlockingQueue`yu kapsüller; `put/take` ve poison pill akışını görünür kılar. |
| `SimulationEngine` | Queue, worker pool, latch, süre ölçümü ve executor yaşam döngüsünü yönetir. |
| `PriceWorker` | Queue'dan görev alır, state ve sayacı günceller, gerçek görev için latch'i azaltır. |
| `ProcessedCounter` | Safe/unsafe sayaçların ortak sözleşmesi. |
| `SafeCounter` | `AtomicLong.incrementAndGet()` kullanır. |
| `UnsafeCounter` | Race condition göstermek için düz `long` ve `count++` kullanır. |
| `CoinState` | `applyDelta` ve immutable `snapshot` davranışını tanımlar. |
| `SafeCoinState` | `currentPrice`, `updateCount`, `lastDelta`, `lastUpdatedBy` alanlarını tek kritik bölümde günceller. |
| `UnsafeCoinState` | Aynı alanları kilitsiz güncelleyerek lost update/tutarsızlık gözlemi sağlar. |
| `ExpectedResultCalculator` | Her coin için expected fiyat ve expected update count değerlerini tek thread ile hesaplar. |
| `InvariantChecker` | Safe fiyat ve update count invariant'larını kontrol eder. |
| `StatsCollector` | Elapsed time, throughput ve coin karşılaştırmalarını immutable `SimulationStats` içinde birleştirir. |
| `WorkerThreadFactory` | `safe-worker-1`, `unsafe-worker-1` gibi anlamlı thread isimleri üretir. |
| `GlobalExceptionHandler` | 400, 404, 409 ve 500 cevaplarını tutarlı `ErrorResponse` formatına dönüştürür. |
| `OpenApiConfig` | Swagger başlık/açıklama metadata'sı; springdoc'un çalışması için zorunlu değildir. |

## 8. Endpoint'ler

### 8.1 Simülasyon Başlatma

```http
POST /simulate?updates=10000&workers=4&seed=42
```

| Parametre | Açıklama | Sınır |
|---|---|---:|
| `updates` | Üretilecek görev sayısı | 1–100.000 |
| `workers` | Worker thread sayısı | 1–16 |
| `seed` | Tekrarlanabilir görev üretimi | İsteğe bağlı |

Akış tamamlanmadan başarılı cevap dönmez:

1. Simülasyon kilidini `AtomicBoolean.compareAndSet(false, true)` ile al.
2. Coin ve sayaç state'lerini başlangıç değerlerine oluştur/sıfırla.
3. Seed'i belirle.
4. Immutable görev listesini yalnızca bir kez üret.
5. Expected sonucu tek thread'de hesapla.
6. Aynı liste ile unsafe simülasyonu çalıştır.
7. Aynı liste ile safe simülasyonu çalıştır.
8. Bütün görevlerin tamamlanmasını bekle.
9. Executor'ları güvenli kapat.
10. Süre, throughput ve invariant sonuçlarını hesapla.
11. Immutable son sonucu güvenli biçimde yayımla.
12. `finally` içinde simülasyon kilidini bırak.

### 8.2 Coin'leri Listeleme

```http
GET /coins
```

Son tamamlanan simülasyondaki safe coin snapshot'larını döndürür. Mutable `SafeCoinState` nesneleri API'ye verilmez.

### 8.3 Son İstatistikleri Görüntüleme

```http
GET /stats
```

Son tamamlanan simülasyonun şu bilgilerini döndürür:

- Kullanılan seed
- Gönderilen görev sayısı
- Worker sayısı
- Unsafe/safe işlenen görev sayıları
- Unsafe/safe elapsed time
- Unsafe/safe throughput
- Safe invariant sonucu
- Her coin için initial/expected/unsafe/safe fiyat
- Her coin için expected/unsafe/safe update count

Henüz simülasyon çalıştırılmadıysa HTTP `404 Not Found` döner.

## 9. Validation ve Hata Cevapları

| Durum | HTTP |
|---|---:|
| `updates < 1` veya `updates > 100000` | 400 |
| `workers < 1` veya `workers > 16` | 400 |
| Sonuç oluşmadan `/stats` veya tercihe göre `/coins` çağrılması | 404 |
| Simülasyon sürerken ikinci `/simulate` isteği | 409 |
| Beklenmeyen execution/shutdown hatası | 500 |

Örnek hata:

```json
{
  "timestamp": "2026-07-12T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Another simulation is already running.",
  "path": "/simulate"
}
```

`GlobalExceptionHandler`, Bean Validation hatalarını da anlaşılır bir mesaj listesiyle dönmelidir.

## 10. Tasarım Kararları

```markdown
### Seed ile Görev Üretimi

`TaskProducer`, istekte kullanılan seed değeriyle bir `Random` nesnesi
oluşturur. Aynı seed ve aynı update sayısı, aynı `PriceUpdateTask` listesini
üretir. Bu sayede hatalar yeniden oluşturulabilir ve expected, unsafe ve safe
çalıştırmalar aynı iş yüküyle karşılaştırılabilir.

Görevler yalnızca bir kez üretilir. Üretilen liste `List.copyOf()` ile
immutable hâle getirilir ve expected, unsafe ve safe simülasyon aşamalarında
yeniden kullanılır.

Task sequence değerleri 1'den başlar. Coin değerleri BTC, ETH ve SOL arasından
seçilir. Delta aralığı takım kararı olarak `-100..100`, sıfır hariç
belirlenmiştir.

### BlockingQueue Seçimi

Producer–Consumer iletişiminde sınırlı kapasiteli `ArrayBlockingQueue`
kullanılmıştır.

Sınırlı kapasite:

- Kuyruğun kontrolsüz biçimde büyümesini engeller.
- Bellek kullanımını sınırlar.
- Producer consumer'lardan hızlıysa `put()` üzerinden backpressure sağlar.
- Queue boşken `take()` worker'ın busy waiting yapmadan beklemesini sağlar.

`LinkedBlockingQueue` da ödev kapsamında kullanılabilir bir alternatifti.
Ancak kapasite belirtilmeden kullanıldığında producer hızlı, consumer yavaşsa
bekleyen görev sayısı çok fazla büyüyebilir. Bu nedenle sınırlı
`ArrayBlockingQueue` tercih edilmiştir.

BlockingQueue yalnızca görevlerin güvenli biçimde dağıtılmasını sağlar.
Queue'dan alınan `CoinState` nesnelerinin thread safety'si ayrıca coin başına
`ReentrantLock` ile sağlanmaktadır.
---------------------------------------------------------------------------------------------------------------------------------


Bu tablo kod tamamlandığında gerçek implementasyonla birebir doğrulanmalıdır.

| Karar noktası | Kararımız | Neden ve alternatif |
|---|---|---|
| Görev üretimi | `Random(seed)` + immutable `List` | Aynı seed ile aynı iş yükünü üretmek ve expected/unsafe/safe karşılaştırmasını geçerli kılmak için |
| Görev kuyruğu | Sınırlı `ArrayBlockingQueue` | Kontrolsüz bellek büyümesini engellemek ve backpressure sağlamak için |
| Queue kapasitesi | `Math.max(1, Math.min(updates, 1_000))` | Küçük çalışmalarda gereksiz büyük kuyruk oluşturmaz; büyük çalışmalarda kuyruğu 1.000 görevle sınırlayıp producer üzerinde gerçek backpressure oluşturur. |
| Worker havuzu | `Executors.newFixedThreadPool(workers, threadFactory)` | Her görev için `new Thread()` açılmaz; thread sayısı sınırlanır ve worker'lar tekrar kullanılır. |
| Güvenli sayaç | `AtomicLong` | Tek değişkenli atomik artırma için lock'tan daha sade ve uygundur. |
| Coin kilidi | `ReentrantLock` | Yönergede en az bir yerde istenir; çok alanlı coin state tek kritik bölümde güncellenir ve `finally` ile bırakılır. |
| Lock kapsamı | Coin başına lock | BTC güncellenirken ETH/SOL gereksiz yere beklemez. Global lock doğru ama concurrency'yi azaltır. |
| İşlerin tamamlanması | `CountDownLatch(taskCount)` + worker başına poison pill | Latch gerçek görevlerin tamamlandığını kanıtlar; poison pill `queue.take()` bekleyen worker'ları düzenli durdurur. |
| Graceful shutdown | `shutdown()` + `awaitTermination()` + timeoutta `shutdownNow()` | Yalnızca `shutdown()` çağırmak işlerin tamamlandığını kanıtlamaz. Interrupt durumu geri yüklenir. |
| Sonucun paylaşılması | Immutable `SimulationStats` + `AtomicReference` | Controller'a yarım/mutable sonuç verilmez; son tamamlanan sonuç güvenli yayımlanır. |
| İkinci simülasyon isteği | `AtomicBoolean.compareAndSet` | Kontrol ve set atomik yapılır; `finally` bloğu hata hâlinde kilidi serbest bırakır. |
| Log seviyesi | Görev logları DEBUG, özet INFO | Benchmark sırasında console I/O ölçümü bozmamalıdır. |

## 11. Race Condition Gözlemi

### 11.1 Unsafe sayaç

```java
private long count;

public void increment() {
    count++;
}
```

`count++` atomik değildir; oku → artır → yaz adımlarından oluşur. İki worker aynı değeri okuyup aynı yeni değeri yazarsa bir artırma kaybolur.

### 11.2 Unsafe coin state

Bir görevin şu alanları birlikte değiştirmesi gerekir:

```text
currentPrice
updateCount
lastDelta
lastUpdatedBy
```

Alanların ayrı ayrı atomik olması bütün coin snapshot'ının tutarlı olduğu anlamına gelmez. Safe sürümde hepsi tek kritik bölümde güncellenir:

```java
lock.lock();
try {
    currentPrice += delta;
    updateCount++;
    lastDelta = delta;
    lastUpdatedBy = Thread.currentThread().getName();
} finally {
    lock.unlock();
}
```

Unsafe sonucu yapay olarak bozulmayacaktır. Race condition her çalıştırmada görünmek zorunda olmadığı için yüksek görev/worker sayısıyla birden fazla stres çalışması raporlanacaktır.

## 12. Invariant ve Doğruluk Kanıtı

Her coin için:

```text
safeCurrentPrice == initialPrice + sum(o coin'e ait bütün delta değerleri)
safeUpdateCount  == o coin için üretilen görev sayısı
```

Toplam sayaç için:

```text
safeProcessedCount == submittedUpdates
```

Örnek teslim çıktısı:

```text
BTC beklenen: 61.240 | safe: 61.240 ✓ | unsafe: 60.890 ✗
BTC görev:    3.342  | safe: 3.342  ✓ | unsafe: 3.268  ✗
Sayaç:       10.000  | safe: 10.000 ✓ | unsafe: 9.784  ✗
```

## 13. Süre ve Throughput

Süre `System.nanoTime()` ile ölçülür:

```text
elapsedSeconds = elapsedNanos / 1_000_000_000.0
throughputPerSecond = processedUpdates / elapsedSeconds
```

Benchmark sırasında görev bazlı DEBUG logları kapatılır. Aynı seed ve update sayısıyla 1, 2, 4 ve 8 worker karşılaştırılır.

| Updates | Workers | Unsafe ms | Safe ms | Unsafe throughput | Safe throughput | Safe invariant |
|---:|---:|---:|---:|---:|---:|---|
| 50.000 | 1 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| 50.000 | 2 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| 50.000 | 4 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |
| 50.000 | 8 | `<...>` | `<...>` | `<...>` | `<...>` | `<...>` |

Yorumda context switching, lock contention ve worker sayısının her zaman daha yüksek performans sağlamadığı tartışılacaktır.

## 14. Thread İsimleri ve Thread Dump

Worker adları mode'u ve sıra numarasını göstermelidir:

```text
unsafe-worker-1
unsafe-worker-2
safe-worker-1
safe-worker-2
```

Thread dump alma yöntemleri:

- IntelliJ IDEA → Run/Debug → Capture Thread Dump
- `jps` ardından `jstack <pid>`
- `jcmd <pid> Thread.print`

README teslim aşamasında şu noktaları yorumlamalıdır:

- `workers` parametresi kadar worker görünüyor mu?
- 10.000 görev için 10.000 thread açılmadığı görülüyor mu?
- Boş worker'lar `BlockingQueue.take()` üzerinde `WAITING` mi?
- Aktif worker'lar `RUNNABLE` mı?
- Lock bekleyen worker'larda contention gözleniyor mu?
- Java-level deadlock var mı?

Thread dump kesiti `docs/thread-dump/` altında saklanabilir.

## 15. Test Planı

### 15.1 Test paketi

```text
src/test/java/com/infina/academy/pricesim
|
|-- engine
|   |-- TaskProducerTest.java
|   `-- SimulationEngineTest.java
|-- counter
|   `-- SafeCounterTest.java
|-- state
|   `-- SafeCoinStateTest.java
|-- metrics
|   |-- ExpectedResultCalculatorTest.java
|   `-- InvariantCheckerTest.java
|-- service
|   `-- SimulationServiceConcurrencyTest.java
`-- api
    `-- SimulationControllerIntegrationTest.java
```

### 15.2 Zorunlu testler

- Aynı seed'in aynı görev listesini üretmesi
- Expected fiyat/update count hesabı
- Safe counter'ın bütün artırmaları koruması
- Safe coin state'in çok alanlı snapshot tutarlılığı
- Safe invariant kontrolü
- `updates/workers` validation → HTTP 400
- En az bir controller integration testi

### 15.3 Ek kalite testleri

- Simülasyon yokken `/stats` → 404
- İkinci eşzamanlı `/simulate` → 409
- Hata sonrasında `AtomicBoolean` kilidinin bırakılması
- Seed verilmezse üretilen seed'in response'a eklenmesi
- Safe processed count'un submitted updates değerine eşit olması
- Worker sayısı kadar poison pill ile havuzun kapanması
- Timeout/interrupt durumunda executor'ın güvenli kapatılması

Unsafe sürümün her testte bozulmasını zorunlu kılan deterministik unit test yazılmayacaktır; bu davranış stres testi ve gözlem raporuyla gösterilir.

## 16. Beş Kişilik Görev Dağılımı

İsimler kesinleşince `<ÜYE-X>` alanları doldurulacaktır. Her üye en az bir anlamlı feature branch, PR ve başka bir üyeye review sağlamalıdır.

| Üye | Ana sorumluluk | Temel sınıflar | Ana branch |
|---|---|---|---|
| `<ÜYE-1>` | Coin & State | `CoinState`, `SafeCoinState`, `UnsafeCoinState`, `CoinSnapshot` | `feature/coin-state` |
| `<ÜYE-2>` | Producer & Worker Pool | `PriceUpdateTask`, `TaskProducer`, `TaskQueue`, `PriceWorker` | `feature/producer-worker-pool` |
| `<ÜYE-3>` | Counter, Expected & Invariant | `ProcessedCounter`, counters, calculator, checker | `feature/counter-invariant` |
| `<ÜYE-4>` | API & Swagger | Controller, DTO, exceptions, validation, OpenAPI, integration test | `feature/simulation-api` |
| `<ÜYE-5>` | Engine, Service & Metrics | `SimulationEngine`, `SimulationService`, stats, shutdown, thread factory | `feature/simulation-engine` |

### Review rotasyonu

| PR sahibi | Birincil reviewer |
|---|---|
| Üye 1 | Üye 2 |
| Üye 2 | Üye 3 |
| Üye 3 | Üye 4 |
| Üye 4 | Üye 5 |
| Üye 5 | Üye 1 |

Ortak sorumluluklar:

- README ve teslim raporunu herkes kontrol eder.
- Her üye kendi alanının testini ve dokümantasyonunu yazar.
- Ortak interface/DTO değişiklikleri PR açıklamasında duyurulur.
- Merge öncesi en az bir approval ve tüm konuşmaların çözülmesi beklenir.

Detaylı iş sırası `docs/ISSUE_PLAN.md` dosyasındadır.

## 17. GitHub Çalışma Düzeni

### 17.1 Issue → Branch → PR

Her geliştirme şu zinciri izler:

```text
GitHub Issue
   -> feature branch
   -> anlamlı commitler
   -> Pull Request
   -> code review
   -> CI başarılı
   -> squash merge
```

PR açıklamasında issue bağlantısı bulunmalıdır:

```text
Closes #12
```

### 17.2 Branch örnekleri

```text
feature/coin-state
feature/producer-worker-pool
feature/counter-invariant
feature/simulation-api
feature/simulation-engine
test/concurrency-scenarios
docs/final-report
```

### 17.3 Commit örnekleri

İyi:

```text
Add immutable price update task
Add bounded blocking task queue
Fix price race with per-coin ReentrantLock
Add simulation conflict response
Add invariant integration test
Resolve README merge conflict
```

Kötü:

```text
fix
deneme
son hali
final2
```

### 17.4 Pull Request kontrolü

Her PR:

- Küçük ve tek amaçlı olmalı.
- İlgili issue'yu bağlamalı.
- Değişiklik ve test açıklaması içermeli.
- Başka bir ekip üyesi tarafından incelenmeli.
- En az bir anlamlı review yorumu almalı veya reviewer açık gerekçeyle onaylamalı.
- CI başarılı olmadan merge edilmemeli.

## 18. GitHub Basic Ruleset

Repo oluşturulup ilk CI çalıştıktan sonra:

```text
Settings -> Rules -> Rulesets -> New branch ruleset
```

Önerilen ayarlar:

- Target branch: `main`
- Require a pull request before merging
- Required approvals: 1
- Require conversation resolution before merging
- Require status checks to pass
- Required check: `build-and-test`
- Block force pushes
- Restrict deletions

Repository merge ayarları:

- Allow squash merging: açık
- Allow merge commits: kapalı
- Automatically delete head branches: açık

İlk repository bootstrap commit'i ruleset açılmadan `main`e gönderilebilir. Ana geliştirmeler ruleset sonrasında doğrudan `main`e push edilmez.

## 19. Planlı Merge Conflict Deneyimi

Zorunlu conflict için uygulama kodu yerine kontrollü bir dokümantasyon satırı kullanılacaktır:

1. `docs/conflict-a` ve `docs/conflict-b` branch'leri açılır.
2. İki branch README'deki aynı “Takım sloganı” satırını farklı değiştirir.
3. İlk PR merge edilir.
4. İkinci PR güncellenirken conflict oluşur.
5. Conflict IntelliJ veya terminalde çözülür.
6. Çözüm commit/PR linki README ve teslim raporuna eklenir.

Teslimde şu bilgiler yazılır:

- Branch isimleri
- Conflict çıkan dosya ve bölüm
- İki branch'in farklı içeriği
- Korunan nihai içerik
- Kullanılan çözüm aracı
- Çözüm commit/PR linki
- Ekipçe öğrenilenler

## 20. Uygulamayı Çalıştırma

### Gereksinimler

- JDK 21
- Maven 3.9+ veya Maven Wrapper
- Git

### Komutlar

```bash
git clone <REPO_URL>
cd infina-concurrent-crypto-simulator
mvn clean test
mvn spring-boot:run
```

Windows Maven Wrapper varsa:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

Örnek akış:

```text
POST /simulate?updates=10000&workers=4&seed=42
GET  /stats
GET  /coins
```

## 21. `application.properties`

```properties
spring.application.name=infina-concurrent-crypto-simulator
server.port=8080

springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html

logging.level.root=INFO
logging.level.com.infina.academy.pricesim=INFO
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

Race gözleminde paket log seviyesi geçici olarak `DEBUG` yapılabilir. Performans tablosu çıkarılırken görev bazlı DEBUG loglar kapalı tutulmalıdır.

## 22. Definition of Done

Bir issue aşağıdakilerin tamamı gerçekleşmeden tamamlanmış sayılmaz:

- Kabul kriterleri karşılandı.
- Kod proje mimarisine uygun yerde.
- Kamuya açık mutable state döndürülmüyor.
- Concurrency aracı neden kullanıldığıyla birlikte anlaşılır.
- İlgili unit/integration testler yazıldı ve geçiyor.
- `mvn clean verify` başarılı.
- Gerekiyorsa Swagger/README güncellendi.
- PR issue'yu bağlıyor.
- En az bir review tamamlandı.
- Review konuşmaları çözüldü.
- CI başarılı.

## 23. Teslim Öncesi Kontrol Listesi

### Uygulama

- [ ] Uygulama hatasız ayağa kalkıyor.
- [ ] `/simulate`, `/coins`, `/stats` çalışıyor.
- [ ] Geçersiz parametreler HTTP 400 dönüyor.
- [ ] Simülasyon yokken `/stats` HTTP 404 dönüyor.
- [ ] İkinci eşzamanlı simülasyon HTTP 409 dönüyor.
- [ ] Her simülasyon başlangıcında safe/unsafe state sıfırlanıyor.

### Concurrency

- [ ] Görevler immutable ve yalnızca bir kez üretiliyor.
- [ ] Expected, safe ve unsafe aynı görev listesini kullanıyor.
- [ ] Kod üzerinde açık `BlockingQueue` bulunuyor.
- [ ] Sabit worker pool kullanılıyor; her görev için thread açılmıyor.
- [ ] Unsafe sayaç ve coin state race condition'ı gerçek biçimde gösteriyor.
- [ ] Safe sayaç `AtomicLong` kullanıyor.
- [ ] Safe coin state en az bir `ReentrantLock` kullanıyor.
- [ ] Lock coin başına ve `finally` içinde bırakılıyor.
- [ ] Bütün görevlerin bitmesi bekleniyor.
- [ ] Poison pill/termination akışı worker'ları durduruyor.
- [ ] Graceful shutdown ve interrupt handling doğru.
- [ ] Son sonuç immutable ve güvenli yayımlanıyor.

### Doğruluk, metrik ve gözlem

- [ ] Seed tekrarlanabilirliği test edildi.
- [ ] Fiyat/update count invariant'ları geçti.
- [ ] Unsafe ve safe süre/throughput ölçüldü.
- [ ] 1/2/4/8 worker tablosu dolduruldu.
- [ ] Görev logları benchmark sırasında kapalı.
- [ ] Thread dump alındı ve yorumlandı.

### Test ve dokümantasyon

- [ ] Unit testler bulunuyor.
- [ ] En az bir controller integration testi bulunuyor.
- [ ] Swagger endpoint açıklamaları ve hata cevapları görünür.
- [ ] README'nin tüm zorunlu başlıkları dolduruldu.
- [ ] `TESLIM_RAPORU.md` dolduruldu.
- [ ] `.gitignore` build/IDE/log/env dosyalarını dışlıyor.

### GitHub ve ekip

- [ ] Her üye en az bir anlamlı feature branch açtı.
- [ ] Her üye en az bir anlamlı PR oluşturdu.
- [ ] Her PR başka bir üye tarafından incelendi.
- [ ] Her üye en az bir code review yorumu yaptı.
- [ ] Ana geliştirmeler doğrudan `main`e gönderilmedi.
- [ ] Commit mesajları anlamlı.
- [ ] En az bir merge conflict oluşturuldu ve çözüldü.
- [ ] Repo ve PR linkleri teslim raporunda bulunuyor.

## 24. Puanlama

| Kriter | Puan |
|---|---:|
| Çalışan sistem ve API | 15 |
| Thread pool ve kuyruk | 20 |
| Race condition ve güvenli çözüm | 20 |
| Tasarım kararları | 10 |
| Test ve validation | 10 |
| GitHub ve ekip çalışması | 15 |
| Metrik, thread dump, Swagger ve doküman | 10 |
| **Toplam** | **100** |

## 25. Bonus Çalışmalar

Zorunlu 100 puan tamamlanmadan bonus yapılmayacaktır.

- Java 21 Virtual Threads karşılaştırması: +4
- `CompletableFuture`: +3
- Deadlock oluşturma ve lock ordering ile çözme: +3

Bonus ana akışı bozmayacak ayrı branch/test/demo üzerinde tutulmalıdır.

## 26. Grup Üyeleri ve Katkıları

| Üye | Sorumluluk | Branch | Pull Request | Review |
|---|---|---|---|---|
| `<ÜYE-1>` | Coin & State | `feature/coin-state` | `<PR>` | `<PR>` |
| `<ÜYE-2>` | Producer & Worker Pool | `feature/producer-worker-pool` | `<PR>` | `<PR>` |
| `<ÜYE-3>` | Counter & Invariant | `feature/counter-invariant` | `<PR>` | `<PR>` |
| `<ÜYE-4>` | API & Swagger | `feature/simulation-api` | `<PR>` | `<PR>` |
| `<ÜYE-5>` | Engine, Service & Metrics | `feature/simulation-engine` | `<PR>` | `<PR>` |

## 27. Teslimde Doldurulacak Kanıtlar

### Race Condition Gözlemi

```text
<Gerçek expected / unsafe / safe sonuçları>
<Kaç çalıştırmanın kaçında unsafe sonuç bozuldu?>
<Worker ve update sayısı arttığında ne gözlendi?>
```

### Thread Dump İncelemesi

```text
<Thread dump'tan kısa kesit>
<Worker sayısı ve state yorumu>
<Contention / deadlock yorumu>
```

### Merge Conflict Deneyimi

```text
Branch'ler:
Dosya/bölüm:
İki değişiklik:
Korunan içerik:
Çözüm aracı:
Commit/PR:
Öğrenilen:
```

### Bonus

```text
Yok / yapılan bonusun yöntemi ve karşılaştırma sonucu
```
