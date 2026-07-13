# AI_PROJECT_GUIDE.md
# İnfina Akademi — Eşzamanlı Kripto Fiyat Simülatörü
## Yapay Zekâ Destekli Geliştirme, İnceleme ve Teslim Kılavuzu

> **Bu dosya projenin yapay zekâ için ana bağlam dosyasıdır.**
>
> Bir ekip üyesi bu dosyayı bir yapay zekâ aracına verdikten sonra yalnızca:
>
> `#15 Add controller integration tests görevini geliştiriyorum`
>
> gibi bir mesaj yazabilmelidir. Yapay zekâ, bu dosyadaki mimariyi ve sınırları koruyarak ilgili görev için:
>
> - Ön koşulları,
> - Dokunulacak dosyaları,
> - Uygulama sırasını,
> - Test senaryolarını,
> - Entegrasyon noktalarını,
> - Kabul kriterlerini,
> - Pull Request kontrol listesini,
> - Ar-Ge inceleme kontrol listesini
>
> eksiksiz şekilde çıkarmalıdır.
>
> Bu dosyada yazan kararlar, bireysel yapay zekâ önerilerinden önceliklidir. Yapay zekâ; yeni teknoloji,
> yeni mimari, veritabanı veya gereksiz sınıf eklemeden önce bu dosyaya uymalıdır.

---

# 1. Yapay Zekâ İçin Çalışma Talimatı

Bu dosyayı okuyan yapay zekâ aşağıdaki kurallara göre hareket etmelidir.

## 1.1 Bir issue istendiğinde izlenecek zorunlu sıra

Kullanıcı bir issue numarası söylediğinde doğrudan rastgele kod üretme. Önce:

1. İlgili issue'nun bu dosyadaki bölümünü bul.
2. Issue sahibini ve bağımlı olduğu issue'ları belirt.
3. Repository'deki mevcut sınıfları incele.
4. Aynı sorumluluğa sahip mevcut sınıf varsa yeni sınıf üretme.
5. Mevcut paket, sınıf ve metot adlarını mümkün olduğunca koru.
6. Eksik bağımlılık varsa kullanıcıyı uyar; başka geliştiricinin alanını habersiz değiştirme.
7. Değiştirilecek ve eklenecek dosyaları listele.
8. Uygulama adımlarını küçük ve doğrulanabilir parçalara ayır.
9. Her adım için test yaklaşımını belirt.
10. Issue dışındaki iyileştirmeleri ayrı öneri olarak sun; issue kapsamına gizlice ekleme.
11. En sonda Definition of Done ve PR kontrol listesi ver.
12. Ar-Ge reviewer'ın kontrol edeceği noktaları ayrıca yaz.

## 1.2 Yapay zekânın cevap formatı

Bir issue için verilen cevap ideal olarak şu başlıkları içermelidir:

```text
1. Görevin amacı
2. Mevcut bağımlılıklar ve kontrol edilmesi gereken sınıflar
3. Dokunulacak dosyalar
4. Sınıf ve sorumluluk tasarımı
5. Adım adım geliştirme planı
6. Kritik concurrency / API / test riskleri
7. Yazılacak testler
8. Lokal doğrulama komutları
9. PR açıklaması örneği
10. Developer Definition of Done
11. Ar-Ge review checklist
12. Bu issue kapsamında yapılmaması gerekenler
```

## 1.3 Repository incelenmeden yapılmaması gerekenler

Yapay zekâ repository'yi görmeden:

- Var olan sınıfın birebir kopyasını oluşturmamalı.
- Metot imzalarını kesinmiş gibi değiştirmemeli.
- Paket isimlerini keyfî şekilde değiştirmemeli.
- Başka issue sahibinin sınıfını yeniden yazmamalı.
- Spring bean döngüsü oluşturacak bağımlılık önermemeli.
- Controller içine concurrency engine kodu koymamalı.
- Engine içine HTTP sınıfları taşımamalı.
- Mutable state nesnelerini API response olarak döndürmemeli.
- Veritabanı/JPA eklememeli.
- Unsafe davranışı yapay olarak bozmamalı.
- Thread testlerinde sonucu garanti etmek için rastgele `Thread.sleep()` kullanmamalı.

## 1.4 Kod önerirken uygulanacak kalite kuralları

- Java 21 kullanılacak.
- Mümkün olan immutable veri taşıyıcıları `record` olarak tasarlanacak.
- Constructor injection tercih edilecek.
- Metotlar tek sorumluluğa yakın tutulacak.
- Anlamlı isimler kullanılacak.
- Gereksiz abstraction oluşturulmayacak.
- Exception yutulmayacak.
- `InterruptedException` sonrasında interrupt flag geri yüklenecek.
- `ReentrantLock.unlock()` her zaman `finally` içinde olacak.
- Thread-safe olmayan koleksiyonlar paylaşılan mutable state için kontrolsüz kullanılmayacak.
- Test edilebilirlik için zaman ve thread davranışları mümkün olduğunca ayrıştırılacak.
- Loglama performans ölçümünü bozmayacak şekilde yapılacak.

---

# 2. Proje Özeti

Bu proje, bellekte çalışan bir **Eşzamanlı Kripto Fiyat Simülatörü**dür.

Sistemde başlangıçta üç coin bulunur:

| Coin | Başlangıç fiyatı |
|---|---:|
| BTC | 60.000 |
| ETH | 3.000 |
| SOL | 150 |

Sistem, belirli bir `seed` kullanarak immutable fiyat güncelleme görevleri üretir.

```java
public record PriceUpdateTask(
        long sequence,
        String coinId,
        long delta
) {
}
```

Aynı görev listesi üç farklı amaçla kullanılır:

1. Beklenen doğru sonuç tek thread ile hesaplanır.
2. Aynı liste unsafe simülasyonda işlenir.
3. Aynı liste safe simülasyonda işlenir.

Amaç, basit bir domain üzerinde aşağıdaki konuları göstermek ve açıklamaktır:

- Task ve thread farkı,
- `ExecutorService`,
- Sabit boyutlu thread pool,
- Producer–Consumer,
- `BlockingQueue`,
- Race condition,
- Lost update,
- `AtomicLong`,
- `ReentrantLock`,
- Coin başına lock,
- `CountDownLatch`,
- Poison pill,
- Graceful shutdown,
- Invariant ile doğruluk kanıtı,
- Süre ve throughput ölçümü,
- Thread dump,
- REST API,
- Swagger,
- GitHub branch, PR, review ve merge conflict süreci.

---

# 3. Proje Kapsamı ve Teknoloji Kararları

## 3.1 Kullanılacak teknolojiler

- Java 21
- Spring Boot
- Maven
- Spring Web
- Spring Validation
- JUnit 5
- Spring Boot Test
- Swagger / OpenAPI (`springdoc-openapi`)
- Git / GitHub

## 3.2 Kullanılmayacak teknolojiler

Bu ödev için aşağıdakiler eklenmeyecektir:

- PostgreSQL
- Hibernate
- Spring Data JPA
- Redis
- Kafka
- Mikroservis
- Docker zorunluluğu
- Kubernetes
- JWT / kullanıcı yönetimi
- Lombok zorunluluğu
- WebSocket
- Haricî kripto API'si

Bu proje **in-memory concurrency simülatörüdür**. Veritabanı eklemek ödev kapsamını genişletir ve
değerlendirilen ana konuyu gölgeler.

## 3.3 Yapılmaması gereken aşırı mühendislik

- Hexagonal architecture'ın tüm katmanlarını yapay olarak üretmek,
- CQRS,
- Event sourcing,
- Generic repository,
- Her sınıf için interface oluşturmak,
- Basit hesapları gereksiz design pattern'lerle bölmek,
- Bir haftalık ödev için mikroservis çıkarmak,
- Her task için `new Thread()` oluşturmak.

---

# 4. Değiştirilemez Proje Kuralları

Aşağıdaki kurallar projenin ana sözleşmesidir.

## 4.1 Görev listesi

- Görevler yalnızca **bir kez** üretilecek.
- Görevler immutable olacak.
- Görev listesi immutable hâle getirilecek.
- Expected, unsafe ve safe aynı listeyi kullanacak.
- Ayrı random üretimleri yapılmayacak.
- Seed verilirse aynı parametreler aynı görev listesini üretmeli.
- Seed verilmezse sistem seed üretmeli ve sonuçta göstermeli.

## 4.2 Producer–Consumer

- Kod üzerinde açıkça `BlockingQueue<PriceUpdateTask>` bulunmalı.
- Yalnızca executor'ın iç kuyruğuna görev göndermek yeterli değildir.
- Worker'lar görevleri queue üzerinden almalı.
- Her görev için yeni thread açılmamalı.
- Worker sayısı `workers` parametresine göre belirlenmeli.
- Worker'lar fixed thread pool ile çalıştırılmalı.

## 4.3 Unsafe yapı

Unsafe yapı gerçekten korunmasız olmalıdır:

- Unsafe sayaç düz `long` ve `count++` kullanabilir.
- Unsafe coin state lock veya atomic compound-state çözümü kullanmamalı.
- Unsafe sonuç elle bozulmamalı.
- Yapay eksiltme, random hata veya task atlama yazılmamalı.
- Unsafe her çalıştırmada yanlış olmak zorunda değildir.

## 4.4 Safe yapı

- Safe sayaçta `AtomicLong` kullanılacak.
- Safe coin state çok alanlı state'i tek kritik bölümde güncelleyecek.
- En az bir yerde `ReentrantLock` kullanılacak.
- Her coin kendi lock'una sahip olacak.
- Global lock kullanılmayacak.
- Aşağıdaki alanlar birlikte güncellenecek:

```text
currentPrice
updateCount
lastDelta
lastUpdatedBy
```

- Snapshot alma işlemi de tutarlı state döndürmeli.

## 4.5 Tamamlanma

- `/simulate`, tüm görevler bitmeden başarılı cevap dönmeyecek.
- Gerçek görev sayısı kadar `CountDownLatch` kullanılabilir.
- Worker sayısı kadar poison pill kullanılabilir.
- Poison pill gerçek task sayılmayacak.
- Yalnızca `shutdown()` yazmak yeterli değildir.
- `shutdown()`, `awaitTermination()` ve gerekirse `shutdownNow()` uygulanacak.
- Interrupt flag geri yüklenecek.

## 4.6 Simülasyon seviyesi concurrency

- Aynı anda yalnızca bir simülasyon çalışacak.
- İkinci `/simulate` isteği HTTP 409 dönecek.
- `AtomicBoolean.compareAndSet(false, true)` benzeri bir kontrol kullanılacak.
- Running flag hata durumunda dahi `finally` içinde serbest bırakılacak.
- Son tamamlanan immutable sonuç güvenli biçimde yayınlanacak.
- Bunun için `AtomicReference` veya eşdeğer güvenli snapshot yaklaşımı kullanılabilir.

## 4.7 Invariant

Her coin için:

```text
safeCurrentPrice
==
initialPrice + o coin'e ait tüm görev deltalarının toplamı
```

ve:

```text
safeUpdateCount
==
o coin için üretilen görev sayısı
```

Genel sayaç için:

```text
safeProcessedCount
==
submittedUpdates
```

Safe sistemin doğruluğu yalnızca “task sayısı doğru” denilerek kanıtlanmış sayılmaz.

## 4.8 Performans

- Süre ölçümünde `System.nanoTime()` tercih edilecek.
- Throughput karşılaştırılabilir iş yükü üzerinden hesaplanmalı.
- Tavsiye edilen formül:

```text
throughputPerSecond = submittedUpdates / elapsedSeconds
```

- 1, 2, 4 ve 8 worker sonuçları karşılaştırılacak.
- Benchmark sırasında task bazlı DEBUG log kapatılacak.
- Daha fazla worker'ın her zaman daha hızlı olmadığı açıklanacak.

## 4.9 Loglama

- Worker isimleri anlamlı olacak:

```text
safe-worker-1
safe-worker-2
unsafe-worker-1
unsafe-worker-2
```

- Task bazlı loglar DEBUG seviyesinde olacak.
- Özet loglar INFO seviyesinde olacak.
- Hassas veya gereksiz büyük veri loglanmayacak.

---

# 5. REST API Sözleşmesi

## 5.1 Simülasyon başlatma

```http
POST /simulate?updates=10000&workers=4&seed=42
```

Parametreler:

| Parametre | Kural |
|---|---|
| `updates` | 1–100.000 |
| `workers` | 1–16 |
| `seed` | Opsiyonel `long` |

Akış:

1. Running kontrolü,
2. State ve sayaç reset,
3. Seed belirleme,
4. Immutable task listesi üretme,
5. Expected hesap,
6. Unsafe çalışma,
7. Safe çalışma,
8. Tüm görevleri bekleme,
9. Executor kapatma,
10. Stats oluşturma,
11. Sonucu güvenli saklama,
12. Response dönme.

## 5.2 Coin listesi

```http
GET /coins
```

Son tamamlanan simülasyonun **safe coin snapshot** listesini döndürür.

Henüz simülasyon yoksa ekip kararı olarak HTTP 404 dönülmesi tercih edilir.

## 5.3 İstatistik

```http
GET /stats
```

En az şu bilgileri içermelidir:

- Seed,
- Submitted updates,
- Worker sayısı,
- Unsafe processed count,
- Safe processed count,
- Unsafe elapsed time,
- Safe elapsed time,
- Unsafe throughput,
- Safe throughput,
- Safe invariant sonucu,
- Her coin için initial, expected, unsafe, safe fiyat,
- Her coin için expected, unsafe, safe update count.

Henüz simülasyon yoksa HTTP 404 dönmelidir.

## 5.4 Hata durumları

| Durum | HTTP |
|---|---:|
| Geçersiz `updates` / `workers` | 400 |
| Sonuç bulunamadı | 404 |
| Başka simülasyon çalışıyor | 409 |
| Beklenmeyen engine hatası | 500 |

Standart hata cevabı mümkün olduğunca aşağıdakileri taşımalıdır:

```json
{
  "timestamp": "2026-07-12T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "Another simulation is already running.",
  "path": "/simulate"
}
```

---

# 6. Nihai Paket ve Sınıf Yapısı

Mevcut repository farklı bir adlandırmaya sahipse gereksiz rename yapılmamalıdır. Ancak sorumluluk sınırları
aşağıdaki yapıya yakın kalmalıdır.

```text
src/main/java/com/infina/academy/pricesim
│
├── PriceSimApplication.java
│
├── api
│   ├── controller
│   │   └── SimulationController.java
│   ├── dto
│   │   ├── CoinResponse.java
│   │   ├── CoinComparisonResponse.java
│   │   ├── SimulationResponse.java
│   │   ├── StatsResponse.java
│   │   └── ErrorResponse.java
│   └── exception
│       ├── GlobalExceptionHandler.java
│       ├── SimulationAlreadyRunningException.java
│       ├── SimulationNotFoundException.java
│       └── SimulationExecutionException.java
│
├── config
│   └── OpenApiConfig.java
│
├── counter
│   ├── ProcessedCounter.java
│   ├── SafeCounter.java
│   └── UnsafeCounter.java
│
├── engine
│   ├── PriceWorker.java
│   ├── SimulationEngine.java
│   ├── SimulationMode.java
│   ├── TaskProducer.java
│   └── TaskQueue.java
│
├── metrics
│   ├── ExpectedResultCalculator.java
│   ├── InvariantChecker.java
│   ├── SimulationStats.java
│   └── StatsCollector.java
│
├── model
│   ├── CoinComparison.java
│   ├── CoinSnapshot.java
│   ├── ExpectedCoinResult.java
│   ├── PriceUpdateTask.java
│   └── SimulationRunResult.java
│
├── service
│   └── SimulationService.java
│
├── state
│   ├── CoinState.java
│   ├── SafeCoinState.java
│   └── UnsafeCoinState.java
│
└── util
    └── WorkerThreadFactory.java
```

Test yapısı:

```text
src/test/java/com/infina/academy/pricesim
│
├── api
│   └── SimulationControllerIntegrationTest.java
├── counter
│   └── SafeCounterTest.java
├── engine
│   ├── TaskProducerTest.java
│   ├── PriceWorkerTest.java
│   └── SimulationEngineTest.java
├── metrics
│   ├── ExpectedResultCalculatorTest.java
│   └── InvariantCheckerTest.java
├── service
│   └── SimulationServiceTest.java
└── state
    └── SafeCoinStateTest.java
```

---

# 7. Katman Sınırları

## 7.1 `model`

- Immutable veri taşıyıcıları,
- Domain sonucu ve snapshot,
- Spring Web bağımlılığı taşımamalı,
- Thread/queue yönetmemeli.

## 7.2 `state`

- Coin'in mutable safe/unsafe çalışma state'i,
- Lock ve snapshot davranışı,
- HTTP veya Swagger bilmemeli,
- Stats oluşturmamalı.

## 7.3 `counter`

- Sadece processed task sayacı,
- Safe ve unsafe davranış,
- Coin state sorumluluğu taşımamalı.

## 7.4 `engine`

- Queue,
- Worker,
- Executor,
- Task işleme,
- Latch,
- Poison pill,
- Shutdown,
- Bir safe veya unsafe run sonucunu üretme.

Engine:

- HTTP response üretmemeli,
- 409 kararı vermemeli,
- Son global sonucu saklamamalı.

## 7.5 `metrics`

- Expected sonuç,
- Invariant,
- Karşılaştırma,
- Süre/throughput raporu.

Metrics katmanı task çalıştırmamalı ve executor yönetmemeli.

## 7.6 `service`

- Bütün akışın orchestrator'ı,
- Task listesi bir kere üretme,
- Expected → unsafe → safe sırasını yönetme,
- Running flag,
- Latest result,
- Engine ve metrics entegrasyonu.

## 7.7 `api`

- Query parametreleri,
- Validation,
- HTTP status,
- DTO mapping,
- Swagger,
- Exception response.

Controller içinde `ExecutorService`, `BlockingQueue`, `ReentrantLock` veya task döngüsü olmamalı.

---

# 8. Ekip Görev Dağılımı

## 8.1 İbrahim

```text
#1 Add immutable task and result models
#2 Implement safe and unsafe coin states
#3 Implement safe and unsafe counters
#4 Add expected calculation and invariant checks
#5 Add core state and metrics unit tests
```

Ana alanları:

- `model`
- `state`
- `counter`
- `metrics`
- Bunların unit testleri

## 8.2 Fırat

```text
#6 Implement seeded task producer
#7 Implement blocking task queue
#8 Implement fixed worker pool and price workers
#9 Add completion and graceful shutdown mechanisms
#10 Add benchmark and thread dump evidence
```

Ana alanları:

- `engine`
- `util/WorkerThreadFactory`
- Concurrency lifecycle
- Benchmark
- Thread dump

## 8.3 Ahmet

```text
#11 Add API DTOs and exception handling
#12 Implement simulation orchestration service
#13 Add simulate, coins and stats endpoints
#14 Add Swagger and request validation
#15 Add controller integration tests
```

Ana alanları:

- `api`
- `service`
- `config`
- API integration testleri

## 8.4 Ortak görevler

```text
#16 Integrate all modules
#17 Perform race condition observation runs
#18 Resolve intentional merge conflict
#19 Complete README and delivery report
#20 Final regression and clean-clone test
```

## 8.5 Ar-Ge

- Cem Bora
- Tolga

Sorumlulukları:

- Issue kabul kriterlerini incelemek,
- PR review yapmak,
- Kanıtları kontrol etmek,
- Merge sonrasında issue'yu manuel kapatmak,
- Dokümantasyon ve gereksinim eşleştirmesini kontrol etmek,
- Clean-clone doğrulamasına katılmak.

Ar-Ge üyelerinin de görünür branch, PR ve review katkısı olmalıdır:

- Cem Bora: README / requirement traceability PR'ı,
- Tolga: TESLIM_RAPORU / final verification PR'ı.

---

# 9. Issue Bağımlılık Haritası

```text
#1 ─┬─> #2 ─┬─> #4 ───────────────┐
    │       └─> #3 ───────┐       │
    │                     │       │
    └─> #6 ─> #7 ─> #8 ─> #9 ───┼─> #12 ─> #13 ─> #14 ─> #15
                    │             │
                    └─> #10       │
                                  └─> #16 ─> #17 ─> #19 ─> #20

#5, #10 ve #15 ilgili modüller geliştikçe paralel ilerleyebilir.
#18 temel repo kurulduktan sonra güvenli bir doküman dosyasında yapılabilir.
```

Pratik merge sırası:

1. `#1`
2. `#6`
3. `#2`
4. `#3`
5. `#7`
6. `#4`
7. `#8`
8. `#9`
9. `#11`
10. `#12`
11. `#13`
12. `#14`
13. Test issue'ları `#5`, `#10`, `#15`
14. `#16`
15. `#17`
16. `#18`
17. `#19`
18. `#20`

Bu sıra kesin bir kilit değildir; ancak sınıf sözleşmeleri hazır olmadan entegrasyon yapılmamalıdır.

---

# 10. Ortak Kod Sözleşmeleri

Bu sözleşmeler issue'ların birbirine uyumunu korumak içindir. Repository'deki mevcut sözleşme farklıysa,
ekip birlikte karar vermeden paralel ve çakışan ikinci sözleşme oluşturulmamalıdır.

## 10.1 Task sözleşmesi

```java
public record PriceUpdateTask(
        long sequence,
        String coinId,
        long delta
) {
}
```

Poison pill için seçenekler:

- `static PriceUpdateTask poisonPill()`,
- Ayrı bir `TaskType`,
- Gerçek sequence aralığının dışında güvenli bir marker.

Yapay zekâ yeni yaklaşım önermeden mevcut implementation'ı kontrol etmelidir.

## 10.2 Coin state davranışı

Ortak sözleşme benzeri:

```java
public interface CoinState {

    String id();

    void apply(long delta, String updatedBy);

    CoinSnapshot snapshot();
}
```

Metot adları mevcut repoya göre değişebilir. Önemli olan safe ve unsafe state'in engine tarafından ortak
sözleşmeyle kullanılabilmesidir.

## 10.3 Counter davranışı

```java
public interface ProcessedCounter {

    void increment();

    long value();
}
```

## 10.4 Engine girdi/çıktı

Engine bir run için en az şunları alabilmelidir:

- Immutable task listesi,
- Worker sayısı,
- Çalışma modu veya state/counter factory.

Engine en az şunları döndürmelidir:

- Coin snapshot listesi,
- Processed count,
- Elapsed time,
- Gerekirse mode bilgisi.

## 10.5 Service sonucu

Service, API'ye safe/unsafe karşılaştırmasını ve genel stats bilgisini tek immutable sonuç olarak sunmalıdır.

---

# 11. ISSUE #1 — Add Immutable Task and Result Models

## Sahip

İbrahim

## Amaç

Simülasyon boyunca paylaşılan girdileri ve dışarı aktarılan sonuçları immutable hâle getirmek.

## Ön koşullar

- Ana Maven/Spring Boot projesi açılmış olmalı.
- Package adı belirlenmiş olmalı.

## Dokunulacak ana alanlar

```text
model/PriceUpdateTask.java
model/CoinSnapshot.java
model/ExpectedCoinResult.java
model/CoinComparison.java
model/SimulationRunResult.java
```

## Yapılması gerekenler

1. `PriceUpdateTask` record oluştur.
2. Coin snapshot'ın gerekli alanlarını belirle:
   - id,
   - initialPrice,
   - currentPrice,
   - updateCount,
   - lastDelta,
   - lastUpdatedBy.
3. Expected sonucu temsil eden immutable model oluştur.
4. Safe/unsafe/expected karşılaştırmasını taşıyan model oluştur.
5. Engine run sonucunu immutable biçimde modelle.
6. Koleksiyon alanlarında defensive copy veya `List.copyOf()` kullan.
7. Mutable state sınıflarını response modeli olarak kullanma.

## Kritik kararlar

- Model sınıfları Spring Web annotation'larına bağımlı olmamalı.
- Gereksiz setter bulunmamalı.
- Aynı kavram için iki farklı model üretilmemeli.
- `CoinState` mutable çalışma nesnesi ile `CoinSnapshot` ayrılmalı.

## Minimum testler

- Task alanları doğru saklanıyor.
- Snapshot alanları doğru saklanıyor.
- Dışarı verilen liste değiştirilemiyor.
- Model equality ihtiyacı varsa record davranışı doğrulanıyor.

## Bu issue kapsamında yapılmaması gerekenler

- Lock kodu yazmak,
- Queue oluşturmak,
- Controller yazmak,
- Stats endpoint yazmak,
- Task üretimi yapmak.

## Ar-Ge kontrolü

- Modeller immutable mı?
- Setter var mı?
- Mutable liste sızıyor mu?
- Yönergede istenen bütün alanlar temsil edilebiliyor mu?
- Sonraki issue'lar bu modelleri tekrar tanımlamak zorunda kalıyor mu?

---

# 12. ISSUE #2 — Implement Safe and Unsafe Coin States

## Sahip

İbrahim

## Amaç

Race condition gösterecek unsafe coin state ile doğru compound update sağlayan safe coin state'i geliştirmek.

## Bağımlılık

- `#1`

## Ana dosyalar

```text
state/CoinState.java
state/UnsafeCoinState.java
state/SafeCoinState.java
model/CoinSnapshot.java
```

## Unsafe davranış

- Lock kullanma.
- `currentPrice += delta`
- `updateCount++`
- `lastDelta = delta`
- `lastUpdatedBy = workerName`
- Yapay task kaybı üretme.
- Random hata ekleme.

## Safe davranış

- Her coin instance'ının kendi `ReentrantLock` nesnesi olsun.
- Dört state alanını aynı kritik bölümde güncelle.
- `unlock()` işlemini `finally` içinde yap.
- Snapshot alma işlemini de tutarlı kıl.
- Global static lock kullanma.

## Yapılması gerekenler

1. Ortak state sözleşmesini tanımla veya mevcut sözleşmeyi kullan.
2. Initial ve current price ayrımını koru.
3. Unsafe implementation oluştur.
4. Safe implementation oluştur.
5. Safe snapshot işleminin yarım state görmediğini garanti et.
6. Reset yerine her run için yeni state yaratılması tercih ediliyorsa bu sözleşmeyi engine/service ile paylaş.
7. Kod içine neden safe/unsafe olduğu anlaşılır şekilde yansıt.

## Testler

- Başlangıç snapshot'ı doğru.
- Tek thread altında safe/unsafe aynı sonucu verir.
- Safe state çoklu thread güncellemelerinde doğru price ve count üretir.
- Snapshot alanları birbiriyle tutarlı.
- Unsafe için “mutlaka yanlış sonuç” assertion'ı yazılmaz.

## Riskler

- `lastUpdatedBy` ile `lastDelta` farklı task'lara ait olabilir.
- Snapshot lock dışında alınırsa safe update'e rağmen tutarsız okunabilir.
- Global lock concurrency'yi gereksiz azaltır.

## Ar-Ge kontrolü

- Gerçekten coin başına lock mu?
- `finally` var mı?
- Unsafe sonuç yapay bozuluyor mu?
- Safe state'in bütün alanları tek kritik bölümde mi?
- Snapshot thread-safe mi?

---

# 13. ISSUE #3 — Implement Safe and Unsafe Counters

## Sahip

İbrahim

## Amaç

`count++` race condition'ını göstermek ve `AtomicLong` ile güvenli sayaç geliştirmek.

## Bağımlılık

- `#1`

## Ana dosyalar

```text
counter/ProcessedCounter.java
counter/UnsafeCounter.java
counter/SafeCounter.java
```

## Yapılması gerekenler

1. Ortak counter sözleşmesi oluştur veya mevcut sözleşmeyi kullan.
2. Unsafe counter'da düz `long` alan kullan.
3. Unsafe increment işleminde `count++` kullan.
4. Safe counter'da `AtomicLong` kullan.
5. Safe increment için `incrementAndGet()` veya eşdeğer atomik işlem kullan.
6. Reset ihtiyacını yeni instance oluşturarak çözmek tercih edilebilir.
7. Counter'ı coin state ile birleştirme.

## Testler

- Safe counter başlangıçta sıfır.
- Tek thread increment doğru.
- Çok thread altında safe counter beklenen toplamı verir.
- Unsafe test flaky başarı/başarısızlık beklentisine dayanmaz.

## Yapılmaması gerekenler

- Unsafe counter'a `volatile` ekleyip güvenliymiş gibi davranmak.
- Unsafe counter'ı `synchronized` yapmak.
- Counter içinde executor oluşturmak.
- Coin update count ile global processed count'u aynı alan yapmak.

## Ar-Ge kontrolü

- Unsafe gerçekten unsafe mı?
- Safe gerçekten AtomicLong mu?
- Counter tek sorumlulukta mı?
- Çoklu thread testi deterministik mi?

---

# 14. ISSUE #4 — Add Expected Calculation and Invariant Checks

## Sahip

İbrahim

## Amaç

Görev listesinden tek thread ile matematiksel doğru sonucu hesaplamak ve safe sonucu invariant'larla doğrulamak.

## Bağımlılıklar

- `#1`
- `#2`
- `#3`

## Ana dosyalar

```text
metrics/ExpectedResultCalculator.java
metrics/InvariantChecker.java
metrics/SimulationStats.java
metrics/StatsCollector.java
model/ExpectedCoinResult.java
model/CoinComparison.java
```

## Yapılması gerekenler

1. BTC, ETH, SOL başlangıç fiyatlarını tek source of truth üzerinden al.
2. Task listesini sırayla gez.
3. Her coin için delta toplamı ve görev sayısını hesapla.
4. Beklenen son fiyatı üret.
5. Safe ve unsafe snapshot'ları expected ile karşılaştır.
6. Price invariant'ını kontrol et.
7. Update count invariant'ını kontrol et.
8. Global processed count invariant'ını kontrol et.
9. Stats içinde hangi coin'in başarısız olduğunu görünür tut.
10. Throughput hesaplamasını ortak utility/metrik sınıfında tut.

## Kritik kurallar

- Expected hesapta concurrency kullanılmaz.
- Expected task listesi safe/unsafe ile aynı liste olmalı.
- Unsafe farklı diye expected değiştirilmez.
- Fiyat ve update count ayrı ayrı doğrulanır.
- Throughput formülü README ile uyumlu olmalı.

## Testler

Known task list:

```text
BTC +100
ETH -20
BTC -50
SOL +4
```

Beklenen:

```text
BTC = 60050, count=2
ETH = 2980, count=1
SOL = 154, count=1
```

Ayrıca:

- Başarılı invariant,
- Fiyatı bozuk snapshot,
- Count'u bozuk snapshot,
- Global processed count hatası,
- Sıfır süreye karşı güvenli throughput davranışı.

## Ar-Ge kontrolü

- Tek thread expected hesap var mı?
- Aynı task listesi kullanılıyor mu?
- Yalnızca processed count kontrol edilmiyor mu?
- Her coin ayrı doğrulanıyor mu?
- Stats gerekli bütün alanları taşıyor mu?

---

# 15. ISSUE #5 — Add Core State and Metrics Unit Tests

## Sahip

İbrahim

## Amaç

Model, state, counter, expected ve invariant bileşenlerini bağımsız testlerle doğrulamak.

## Bağımlılıklar

- `#1`
- `#2`
- `#3`
- `#4`

## Test kapsamı

```text
PriceUpdateTaskTest
SafeCoinStateTest
SafeCounterTest
ExpectedResultCalculatorTest
InvariantCheckerTest
```

## Zorunlu senaryolar

- Immutable task davranışı,
- Safe coin başlangıç durumu,
- Safe coin tek update,
- Safe coin concurrent update,
- Safe snapshot tutarlılığı,
- Safe counter concurrent increment,
- Expected price,
- Expected update count,
- Price invariant,
- Count invariant,
- Processed count invariant.

## Test tasarım kuralları

- Testler birbirinden bağımsız olmalı.
- Thread executor test sonunda kapatılmalı.
- Sonsuz bekleme riski olmamalı.
- Timeout kullanılabilir.
- Rastgele sleep yerine latch/barrier tercih edilmeli.
- Unsafe her seferinde yanlış çıkmalı diye assertion yapılmamalı.
- Test içinde production state'i yapay değiştirmekten kaçınılmalı.

## Ar-Ge kontrolü

- Testler gerçekten thread safety'yi doğruluyor mu?
- Flaky test var mı?
- Executor leak var mı?
- İsimler davranışı açıklıyor mu?
- `mvn test` tekrar tekrar başarılı mı?

---

# 16. ISSUE #6 — Implement Seeded Task Producer

## Sahip

Fırat

## Amaç

Tekrarlanabilir, immutable ve tek seferlik görev listesi üretmek.

## Bağımlılık

- `#1`

## Ana dosya

```text
engine/TaskProducer.java
```

## Yapılması gerekenler

1. `updates` kadar task üret.
2. Seed ile `Random` veya uygun seeded generator kullan.
3. Coin seçimlerini BTC/ETH/SOL ile sınırla.
4. Delta aralığını proje içinde sabitle ve dokümante et.
5. Sequence değerlerini açık ve benzersiz üret.
6. Dönen listeyi immutable yap.
7. Task üretimi sırasında state değiştirme.
8. Safe ve unsafe için ayrı üretim yapma.
9. Seed yoksa seed üretme sorumluluğunu service'e bırakmak tercih edilir.

## Testler

- Aynı seed + aynı updates → aynı task listesi.
- Farklı seed → çoğunlukla farklı task listesi.
- Task count doğru.
- Sequence doğru.
- Coin id'ler geçerli.
- Liste değiştirilemiyor.

## Riskler

- `ThreadLocalRandom` ile seed tekrarlanabilirliği kaybolur.
- Task listesi her run aşamasında yeniden üretilirse karşılaştırma bozulur.
- Mutable task kullanılırsa safe/unsafe arasında veri değişebilir.

## Ar-Ge kontrolü

- Seed gerçekten etkili mi?
- Task bir kere üretilebilecek sözleşmede mi?
- Liste immutable mı?
- Sequence ve coin id doğru mu?

---

# 17. ISSUE #7 — Implement Blocking Task Queue

## Sahip

Fırat

## Amaç

Producer ile worker'ları açık bir `BlockingQueue` üzerinden ayırmak.

## Bağımlılıklar

- `#1`
- `#6`

## Ana dosya

```text
engine/TaskQueue.java
```

## Tasarım kararı

`ArrayBlockingQueue` veya `LinkedBlockingQueue` kullanılabilir. Seçim README'de açıklanmalıdır.

Önerilen güvenli yaklaşım:

- Worker'lar önce başlatılır.
- Producer caller thread üzerinden queue'ya `put()` eder.
- Sınırlı queue kullanılıyorsa consumer'lar producer'ı rahatlatır.
- Son görevlerden sonra worker sayısı kadar poison pill eklenir.

## Yapılması gerekenler

1. Queue tipini belirle.
2. Kapasite politikasını belirle.
3. `put()` / `take()` kullan.
4. Busy waiting kullanma.
5. Poison pill yaklaşımını mevcut task modeliyle uyumlu kur.
6. Poison pill'in expected ve stats hesaplarına girmesini engelle.
7. Queue'yu static global paylaşma.
8. Her safe/unsafe run için bağımsız queue oluştur.

## Testler

- Put/take.
- FIFO davranışı.
- Poison pill ayırt etme.
- Bounded queue varsa producer blocking davranışı için kontrollü test.
- Interrupted take davranışı.

## Yapılmaması gerekenler

```java
while (queue.isEmpty()) {
}
```

veya:

```java
ConcurrentLinkedQueue
```

ile busy polling.

## Ar-Ge kontrolü

- Kod üzerinde BlockingQueue açık mı?
- Executor iç kuyruğuna güvenilip geçilmiş mi?
- Poison pill doğru mu?
- Queue seçimi gerekçeli mi?
- Bellek büyümesi riski tartışılmış mı?

---

# 18. ISSUE #8 — Implement Fixed Worker Pool and Price Workers

## Sahip

Fırat

## Amaç

Sabit sayıda, anlamlı isimli worker ile queue'daki görevleri işlemek.

## Bağımlılıklar

- `#2`
- `#3`
- `#7`

## Ana dosyalar

```text
engine/PriceWorker.java
engine/SimulationMode.java
util/WorkerThreadFactory.java
```

## Worker sorumlulukları

- Queue'dan `take()` ile task al.
- Poison pill ise dur.
- Task coin'ini bul.
- State update uygula.
- Global processed counter'ı artır.
- Completion mekanizmasına haber ver.
- DEBUG log yaz.
- Interrupt durumunda düzgün sonlan.

## Thread factory

Thread isimleri:

```text
safe-worker-1
safe-worker-2
unsafe-worker-1
unsafe-worker-2
```

Worker sayısı `workers` parametresiyle aynı olmalı.

## Kritik hata yönetimi

Task işleme sırasında exception olsa bile gerçek task için completion signal kaybolmamalıdır.

Uygun yaklaşım:

```java
try {
    process(task);
} finally {
    completionLatch.countDown();
}
```

Ancak poison pill için countDown yapılmamalıdır.

## Testler

- Thread isimleri.
- Normal task işleme.
- Safe state kullanımı.
- Unsafe state kullanımı.
- Counter increment.
- Poison pill durma.
- Interrupt flag.

## Riskler

- Worker exception ile sessizce ölürse latch sonsuza kadar bekleyebilir.
- Poison pill sayısı worker sayısından azsa worker kalabilir.
- Poison pill sayısı fazlaysa gereksiz task kalabilir.
- Worker state'i yanlış coin map ile paylaşılırsa sonuç bozulur.

## Ar-Ge kontrolü

- Fixed pool mu?
- Her task için thread açılıyor mu?
- Worker isimleri doğru mu?
- Interrupt doğru mu?
- Poison pill doğru mu?
- Completion garanti altında mı?

---

# 19. ISSUE #9 — Add Completion and Graceful Shutdown Mechanisms

## Sahip

Fırat

## Amaç

Safe ve unsafe run'ların bütün görevlerini tamamlayıp kaynak bırakmadan sonuç üretmesini sağlamak.

## Bağımlılıklar

- `#7`
- `#8`

## Ana dosya

```text
engine/SimulationEngine.java
```

## Engine akışı

1. Yeni coin state seti oluştur.
2. Yeni processed counter oluştur.
3. BlockingQueue oluştur.
4. CountDownLatch oluştur.
5. Fixed thread pool oluştur.
6. Worker'ları submit et.
7. Task'ları queue'ya koy.
8. Worker sayısı kadar poison pill koy.
9. Latch'i bekle.
10. Executor shutdown başlat.
11. Await termination.
12. Timeout olursa shutdownNow.
13. Snapshot'ları topla.
14. Elapsed time hesapla.
15. Immutable `SimulationRunResult` döndür.

## Graceful shutdown örüntüsü

```java
executor.shutdown();

try {
    if (!executor.awaitTermination(timeout, unit)) {
        executor.shutdownNow();

        if (!executor.awaitTermination(timeout, unit)) {
            // gerekli log / execution error
        }
    }
} catch (InterruptedException exception) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

## Kritik kurallar

- `/simulate` engine bitmeden devam etmemeli.
- Safe ve unsafe run birbirinden bağımsız state kullanmalı.
- Task listesi aynı referans/aynı içerik olmalı.
- Executor field olarak uzun ömürlü tutulmak zorunda değil.
- Thread leak olmamalı.
- Poison pill gerçek count'a girmemeli.

## Testler

- Tek worker ile bütün task'lar.
- Çok worker ile bütün task'lar.
- Safe run invariant'a uygun.
- Latch tamamlanıyor.
- Executor terminate oluyor.
- Interrupt senaryosu.
- Timeout davranışı mümkünse kontrollü test.

## Ar-Ge kontrolü

- Sadece `shutdown()` mı yazılmış?
- Await var mı?
- Timeout var mı?
- Interrupt flag geri yüklenmiş mi?
- Run sonrası worker kalıyor mu?
- Result tamamlanmadan dönüyor mu?

---

# 20. ISSUE #10 — Add Benchmark and Thread Dump Evidence

## Sahip

Fırat

## Amaç

Worker sayısının performansa etkisini ve JVM thread davranışını gerçek kanıtla raporlamak.

## Bağımlılıklar

- `#9`
- Entegrasyon için tercihen `#12` ve `#13`

## Benchmark planı

Aynı:

- Seed,
- Updates,
- Başlangıç fiyatları,
- Task listesi,
- Log seviyesi

kullanılmalı.

Öneri:

```text
updates=50000
seed=42
workers=1,2,4,8
```

Her çalışma için:

- Unsafe elapsed,
- Safe elapsed,
- Unsafe throughput,
- Safe throughput,
- Safe invariant,
- CPU/ortam notu.

## Race condition gözlemi

Öneri:

```text
updates=100000
workers=8
seed=42
20 tekrar
```

Raporda:

- Unsafe counter kaç kez sapmış?
- Unsafe price kaç kez sapmış?
- Worker=1 ile sapma görüldü mü?
- Worker artınca contention ne oldu?

## Thread dump

Yöntemler:

```text
IntelliJ Capture Thread Dump
jcmd <pid> Thread.print
jstack <pid>
```

Bakılacaklar:

- Worker sayısı,
- Worker adları,
- WAITING thread'ler,
- Queue take üzerinde bekleme,
- Lock contention,
- Deadlock,
- Kontrolsüz binlerce thread olmaması.

## Kanıt dosyaları

Önerilen:

```text
docs/evidence/benchmark-results.md
docs/evidence/thread-dump.txt
docs/evidence/race-observation.md
```

## Kritik kural

Task bazlı DEBUG log benchmark sırasında kapalı olmalı.

## Ar-Ge kontrolü

- Aynı seed ve updates mi?
- Gerçek değerler mi?
- Sonuçlar uydurulmuş mu?
- Worker sayısı dump ile uyumlu mu?
- Deadlock var mı?
- README yorumu teknik olarak doğru mu?

---

# 21. ISSUE #11 — Add API DTOs and Exception Handling

## Sahip

Ahmet

## Amaç

Domain/state nesnelerini dışarı sızdırmadan anlaşılır API response'ları ve standart hata cevapları oluşturmak.

## Bağımlılıklar

- `#1`
- `#4`

## Ana dosyalar

```text
api/dto/CoinResponse.java
api/dto/CoinComparisonResponse.java
api/dto/SimulationResponse.java
api/dto/StatsResponse.java
api/dto/ErrorResponse.java

api/exception/GlobalExceptionHandler.java
api/exception/SimulationAlreadyRunningException.java
api/exception/SimulationNotFoundException.java
api/exception/SimulationExecutionException.java
```

## Yapılması gerekenler

1. API response alanlarını assignment ile eşleştir.
2. DTO'ları immutable tasarla.
3. Mutable `CoinState` döndürme.
4. Domain-to-DTO mapping'i controller'a dağınık kopyalama; sade bir mapper/helper kullanılabilir.
5. 400, 404, 409, 500 error response standardı oluştur.
6. Validation exception handler ekle.
7. İç exception stack trace'ini response'a koyma.
8. Mesajları anlaşılır yap.

## Testler

- DTO mapping.
- 404 error body.
- 409 error body.
- Validation error body.
- 500 error body için kontrollü senaryo.

## Riskler

- DTO'da eksik expected/unsafe/safe alanı.
- Coin state'i doğrudan serialize etmek.
- Exception handler'ın bütün exception'ları yanlış status ile yakalaması.
- Validation mesajlarının boş dönmesi.

## Ar-Ge kontrolü

- Assignment için gerekli bütün response alanları var mı?
- Hata statüleri doğru mu?
- Mutable state dışarı çıkıyor mu?
- Response isimleri anlaşılır mı?

---

# 22. ISSUE #12 — Implement Simulation Orchestration Service

## Sahip

Ahmet

## Amaç

Task production, expected calculation, unsafe run, safe run, invariant ve latest result işlemlerini tek
application service akışında birleştirmek.

## Bağımlılıklar

- `#4`
- `#6`
- `#9`
- `#11`

## Ana dosya

```text
service/SimulationService.java
```

## Zorunlu akış

```text
AtomicBoolean running guard
        ↓
seed belirle
        ↓
task listesi bir kez üret
        ↓
expected hesapla
        ↓
unsafe engine run
        ↓
safe engine run
        ↓
stats + invariant
        ↓
latest immutable result publish
        ↓
running flag finally false
```

## Yapılması gerekenler

1. `AtomicBoolean simulationRunning` oluştur.
2. `compareAndSet(false, true)` ile giriş kontrolü yap.
3. Başarısızsa `SimulationAlreadyRunningException`.
4. Seed belirle.
5. Task listesi **bir kez** üret.
6. Expected hesapla.
7. Aynı task listesiyle unsafe run.
8. Aynı task listesiyle safe run.
9. Stats oluştur.
10. Safe invariant sonucu ekle.
11. Immutable latest result'ı `AtomicReference` ile sakla.
12. `/coins` için safe snapshot'ları latest result'tan çıkar.
13. Sonuç yoksa `SimulationNotFoundException`.
14. Running flag'i `finally` içinde kapat.

## Tasarım sınırları

- Service HTTP status bilmek zorunda değil.
- Service controller DTO'suna bağımlı olmamalı.
- Service executor ayrıntısını yönetmemeli; engine'e delegasyon yapmalı.
- Engine global latest result saklamamalı.
- Task listesi safe/unsafe arasında kopyalanıp değiştirilmemeli.

## Testler

- Aynı seed ile aynı expected.
- İkinci eşzamanlı çağrı exception.
- Engine exception sonrası flag serbest.
- Latest result saklanıyor.
- Safe coins dönüyor.
- No-result exception.
- Task producer bir kez çağrılıyor.
- Unsafe ve safe engine'e aynı task listesi veriliyor.

## Ar-Ge kontrolü

- Task producer kaç kez çağrılıyor?
- Running flag finally içinde mi?
- Same-list kuralı korunuyor mu?
- Latest result güvenli mi?
- Layer sınırları korunuyor mu?

---

# 23. ISSUE #13 — Add Simulate, Coins and Stats Endpoints

## Sahip

Ahmet

## Amaç

Service işlemlerini üç zorunlu REST endpoint üzerinden sunmak.

## Bağımlılıklar

- `#11`
- `#12`

## Ana dosya

```text
api/controller/SimulationController.java
```

## Endpoint'ler

```http
POST /simulate
GET /coins
GET /stats
```

## Yapılması gerekenler

1. Query parametrelerini al.
2. Service'i çağır.
3. Domain/service sonucunu DTO'ya dönüştür.
4. Başarılı response dön.
5. HTTP status yönetimini exception handler ile uyumlu tut.
6. Controller'ı ince tut.
7. Controller içinde executor veya queue kurma.
8. `/simulate` sync çalışmalı ve tam sonuç dönmeli.

## Testler

- Mock service ile controller unit testi opsiyonel.
- Gerçek integration test #15 kapsamındadır.
- Endpoint mapping doğrulanmalı.

## Ar-Ge kontrolü

- Üç endpoint var mı?
- Path ve HTTP method doğru mu?
- Controller ince mi?
- Safe coins mı dönüyor?
- Stats alanları tam mı?

---

# 24. ISSUE #14 — Add Swagger and Request Validation

## Sahip

Ahmet

## Amaç

Parametre sınırlarını doğrulamak ve API'nin Swagger üzerinden anlaşılır/test edilebilir olmasını sağlamak.

## Bağımlılık

- `#13`

## Ana dosyalar

```text
api/controller/SimulationController.java
config/OpenApiConfig.java
application.properties
pom.xml
```

## Validation

```text
updates: @Min(1), @Max(100000)
workers: @Min(1), @Max(16)
seed: optional
```

Method validation gerekiyorsa uygun Spring annotation/config eklenmelidir.

## Swagger

Her endpoint için:

- Özet,
- Açıklama,
- Parametre açıklaması,
- 200 response,
- 400/404/409/500 response,
- Örnek kullanım.

Swagger adresi README'de gerçek çalışan adresle aynı olmalı:

```text
http://localhost:8080/swagger-ui/index.html
```

## Testler

- `updates=0` → 400.
- `updates=100001` → 400.
- `workers=0` → 400.
- `workers=17` → 400.
- Geçerli parametre → validation geçer.
- Swagger docs endpoint erişilebilirliği smoke test olarak değerlendirilebilir.

## Riskler

- `@Min` koyup method validation'ı aktif etmemek.
- Exception handler'ın validation body üretmemesi.
- Swagger dependency sürümünün Spring Boot ile uyumsuz olması.
- README'de yanlış swagger path.

## Ar-Ge kontrolü

- Dört sınır testi geçiyor mu?
- Swagger açılıyor mu?
- Parametreler açıklanmış mı?
- Error response'lar dokümante mi?

---

# 25. ISSUE #15 — Add Controller Integration Tests

## Sahip

Ahmet

## Amaç

REST API'yi Spring context, validation, exception handling ve service entegrasyonu ile gerçekçi şekilde doğrulamak.

## Bağımlılıklar

- `#11`
- `#12`
- `#13`
- `#14`
- Tercihen `#16` entegrasyon branch'i

## Ana test dosyası

```text
src/test/java/.../api/SimulationControllerIntegrationTest.java
```

Gerekirse test konfigürasyonları:

```text
src/test/resources/application-test.properties
```

## Yapay zekâ bu issue için önce neyi incelemeli?

1. Controller path'leri.
2. Service bean yapısı.
3. DTO JSON alanları.
4. Exception handler.
5. Validation annotation'ları.
6. Simulation'ın çok hızlı bitip bitmediği.
7. 409 testinin nasıl deterministik yapılacağı.
8. Latest result state'inin testler arasında nasıl temizleneceği.

## Zorunlu test senaryoları

### Başarılı simülasyon

```http
POST /simulate?updates=1000&workers=4&seed=42
```

Kontroller:

- HTTP 200,
- Seed 42,
- Submitted updates 1000,
- Workers 4,
- Safe processed 1000,
- Safe invariant true,
- BTC/ETH/SOL karşılaştırmaları mevcut.

### Parametre validation

- `updates=0` → 400,
- `updates=-1` → 400,
- `updates=100001` → 400,
- `workers=0` → 400,
- `workers=17` → 400.

### Sonuç bulunamadı

Fresh context veya temizlenmiş latest result ile:

- `GET /stats` → 404,
- Proje kararına göre `GET /coins` → 404.

### Simülasyon sonrası sorgular

Başarılı `/simulate` sonrasında:

- `GET /stats` → 200,
- `GET /coins` → 200,
- Üç coin mevcut,
- Dönen coin'ler safe snapshot.

### Aynı anda ikinci istek

İlk simülasyon deterministik şekilde bloklanırken ikinci istek:

- HTTP 409.

## 409 testinde önemli kural

Rastgele:

```java
Thread.sleep(100);
```

kullanarak ilk request'in hâlâ çalıştığını varsayma.

Tercih edilen yöntemler:

- Test-specific blocking fake/spy engine,
- `CountDownLatch` ile ilk çağrıyı kontrollü tutma,
- Service testinde concurrency guard'ı ayrı doğrulayıp controller testinde exception mapping doğrulama,
- Spring bean override ile deterministic test double.

Repository yapısına göre en az karmaşık yaklaşım seçilmelidir.

## Test izolasyonu

- Latest result testler arasında sızmamalı.
- Running flag testler arasında true kalmamalı.
- Her test bağımsız çalışmalı.
- Test sırasına güvenilmemeli.
- Geride executor thread kalmamalı.

## Kullanılabilecek araçlar

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `MockMvc`
- JSON path assertions
- Gerekirse test bean override / mock bean
- JUnit timeout

## Kabul kriterleri

- En az bir gerçek controller integration testi.
- 200, 400, 404, 409 doğrulanıyor.
- Başarılı response'ta invariant kontrol ediliyor.
- Seed JSON'da kontrol ediliyor.
- Safe coins kontrol ediliyor.
- Testler sıradan bağımsız.
- Flaky sleep yok.
- `mvn test` ve `mvn verify` başarılı.
- Executor/thread leak yok.

## Bu issue kapsamında yapılmaması gerekenler

- Production service'i test kolaylığı için bozmak.
- Validation limitlerini değiştirmek.
- Controller contract'ını keyfî değiştirmek.
- Unsafe sonucu yanlış olmaya zorlayan assertion.
- Gerçek 100.000 task ile her test run'ını aşırı yavaşlatmak.

## Ar-Ge kontrolü

- Integration test gerçek Spring context kullanıyor mu?
- Sadece controller mock testi yazılıp “integration” denmiş mi?
- 409 deterministik mi?
- Test sırası değişince geçiyor mu?
- CI üzerinde stabil mi?
- Response alanları assignment ile uyumlu mu?

---

# 26. ISSUE #16 — Integrate All Modules

## Sahip

Ortak

## Ana koordinasyon

Ahmet service/API entegrasyonunu yönetebilir; fakat üç geliştirici kendi modülünün hatalarından sorumludur.

## Amaç

Model, state, counter, producer, queue, worker, engine, metrics, service ve API modüllerini tek çalışan
uygulamada birleştirmek.

## Yapılması gerekenler

1. Main'i güncelle.
2. Modül PR'larını doğru sırayla merge et.
3. Çakışan sınıf ve duplicate model var mı kontrol et.
4. Bean bağımlılıklarını kontrol et.
5. Service constructor'ını gerçek component'lerle bağla.
6. Safe/unsafe engine factory yaklaşımını netleştir.
7. DTO mapping'i tamamla.
8. `mvn clean verify` çalıştır.
9. Swagger'dan uçtan uca simülasyon çalıştır.
10. Thread leak ve shutdown kontrolü yap.

## Entegrasyon kontrol listesi

- Aynı task listesi,
- Yeni state per run,
- Safe ve unsafe bağımsız,
- Coin başına lock,
- Queue açık,
- Fixed pool,
- Latch,
- Poison pill,
- Shutdown,
- Latest result,
- 400/404/409,
- Swagger,
- Tests.

## Yapılmaması gerekenler

- Entegrasyon hatasını çözmek için bütün modülleri tek sınıfta birleştirmek.
- Çalışsın diye safe/unsafe ayrımını kaldırmak.
- Testleri silmek.
- Issue sahibine danışmadan public contract değiştirmek.

## Ar-Ge kontrolü

- Uçtan uca çalışıyor mu?
- Zorunlu yapıların hiçbiri entegrasyon sırasında kaybolmuş mu?
- Kod sorumlulukları hâlâ ayrılmış mı?
- CI başarılı mı?

---

# 27. ISSUE #17 — Perform Race Condition Observation Runs

## Sahip

Ortak

## Ana teknik sorumlu

Fırat

## Amaç

Unsafe yapının zamanlamaya bağlı sapmalarını gerçek deneylerle gözlemlemek ve raporlamak.

## Deney matrisi

Örnek:

| Updates | Workers | Seed | Tekrar |
|---:|---:|---:|---:|
| 100.000 | 1 | 42 | 5 |
| 100.000 | 2 | 42 | 10 |
| 100.000 | 4 | 42 | 20 |
| 100.000 | 8 | 42 | 20 |

## Kaydedilecekler

- Unsafe processed count sapması,
- Her coin unsafe price sapması,
- Her coin unsafe update count sapması,
- Safe invariant,
- Sapma görülen run sayısı,
- Worker sayısının etkisi.

## Kritik kural

Unsafe sonucu bozdurmak için:

- Sleep ekleme,
- Task atlama,
- Random decrement,
- Manuel yanlış değer

yapılmaz.

Thread scheduling'i görünür kılmak için devasa log açmak da benchmark'ı bozar.

## Ar-Ge kontrolü

- Deney tekrarlanabilir mi?
- Seed ve parametreler yazılı mı?
- Sonuçlar gerçek mi?
- Safe her run invariant sağlıyor mu?
- Worker=1 yorumu doğru mu?

---

# 28. ISSUE #18 — Resolve Intentional Merge Conflict

## Sahip

Ortak

## Amaç

Gerçek bir merge conflict oluşturmak, çözmek ve belgelemek.

## Güvenli dosya

Ana production kodunu bozmak yerine:

```text
docs/conflict-demo.md
```

kullanılabilir.

## Adımlar

1. İki branch aynı main commit'inden açılsın.
2. Aynı satırı farklı içerikle değiştirsin.
3. İlk PR merge edilsin.
4. İkinci branch main ile güncellensin.
5. Conflict görülsün.
6. Manuel çözülsün.
7. Çözüm commit'i atılsın.
8. İkinci PR merge edilsin.
9. README'ye kanıt eklenir.

## README bilgileri

- Branch isimleri,
- Dosya,
- Satır/bölüm,
- İki farklı değişiklik,
- Korunan içerik,
- Çözüm aracı,
- Commit/PR linki,
- Öğrenilenler.

## Ar-Ge kontrolü

- Gerçek conflict oluşmuş mu?
- Aynı satır mı?
- Conflict marker kalmış mı?
- CI geçiyor mu?
- Doküman tamam mı?

---

# 29. ISSUE #19 — Complete README and Delivery Report

## Sahip

Ortak

## Ar-Ge PR sahipliği

- Cem Bora: README ve requirements traceability.
- Tolga: TESLIM_RAPORU ve final evidence bölümleri.

## README zorunlu başlıkları

- Proje Hakkında
- Kullanılan Teknolojiler
- Uygulamayı Çalıştırma
- Swagger Adresi
- Endpoint'ler
- Mimari Akış
- Tasarım Kararları
- Race Condition Gözlemi
- Güvenli Çözüm
- Invariant
- Performans
- ReentrantLock vs synchronized
- Thread Dump
- Merge Conflict
- Testler
- Grup Üyeleri ve Katkıları
- Bonuslar

## Dokuz tasarım kararı

1. Görev kuyruğu,
2. Worker havuzu,
3. Güvenli sayaç,
4. Coin kilidi,
5. Lock kapsamı,
6. İşlerin tamamlanması,
7. Graceful shutdown,
8. Sonucun paylaşılması,
9. İkinci simülasyon isteği.

Her biri için:

- Seçim,
- Neden,
- Alternatif,
- Alternatif neden seçilmedi.

## Requirements traceability

```text
Gereksinim → Issue → PR → Test/Kanıt → Durum
```

## TESLIM_RAPORU

- Grup bilgileri,
- Repo linki,
- PR linkleri,
- Conflict,
- Kısa açıklama,
- Çalıştırma,
- Tasarım kararları,
- Race kanıtı,
- Metrik,
- Thread dump,
- Öz değerlendirme,
- Bireysel katkı.

## Ar-Ge kontrolü

- Placeholder kalmış mı?
- Sahte örnek değer kalmış mı?
- Linkler çalışıyor mu?
- Çalıştırma komutları doğru mu?
- Her üyenin branch, PR ve review'u var mı?

---

# 30. ISSUE #20 — Final Regression and Clean-Clone Test

## Sahip

Ortak

## Final doğrulama liderleri

Cem Bora ve Tolga

## Amaç

Projeyi değerlendiren kişinin sıfırdan çalıştırabileceğini kanıtlamak.

## Clean clone adımları

```bash
git clone <repository-url>
cd <repository>
mvn clean verify
mvn spring-boot:run
```

Kontroller:

```text
Swagger açılıyor
POST /simulate çalışıyor
GET /stats çalışıyor
GET /coins çalışıyor
400 çalışıyor
404 çalışıyor
409 çalışıyor
Safe invariant true
```

## Repository hijyeni

- `target/` yok,
- `.idea/` yok,
- `.env` yok,
- Secret yok,
- Bozuk link yok,
- Placeholder yok,
- Untracked gerekli dosya yok,
- CI green,
- Main korumalı.

## Final regression

- Seed reproducibility,
- Safe counter,
- Safe state,
- Same-list,
- Queue,
- Worker count,
- Shutdown,
- Invariant,
- Endpoints,
- Swagger,
- Tests,
- Thread dump evidence,
- Merge conflict evidence.

## Final kanıt dosyası

Tolga veya Cem Bora aşağıdaki dosyayı PR ile ekleyebilir:

```text
docs/FINAL_VERIFICATION.md
```

İçinde:

- Final commit hash,
- Test sonucu,
- CI linki,
- Swagger doğrulaması,
- Örnek simulate sonucu,
- Known issues: none / list,
- Teslim onayı.

## Ar-Ge kontrolü

- Temiz makine/klasörde çalıştı mı?
- Yalnız geliştiricinin IDE'sinde çalışan gizli ayar var mı?
- CI ile lokal sonuç uyumlu mu?
- Teslime engel açık issue var mı?

---

# 31. Geliştiriciler Arası Entegrasyon Protokolü

## 31.1 Başka geliştiricinin alanına dokunma

Başka geliştiricinin public contract'ında değişiklik gerekirse:

1. Issue yorumunda nedeni yaz.
2. İlgili geliştiriciyi etiketle.
3. Yeni sözleşmeyi birlikte netleştir.
4. Küçük contract PR'ını önce merge et.
5. Sonraki feature'ları bunun üzerine rebase et.

## 31.2 Duplicate sınıf oluşmasını engelleme

PR açmadan önce ara:

```text
PriceUpdateTask
CoinSnapshot
SimulationStats
ErrorResponse
SimulationResult
```

Aynı sorumluluk varsa ikinci sınıf oluşturma.

## 31.3 Mapping sorumluluğu

- State → snapshot: state/model sınırı,
- Service result → API DTO: api tarafı,
- Expected + run results → stats: metrics/service sınırı.

## 31.4 Bean bağımlılıkları

Önerilen yön:

```text
Controller → SimulationService
SimulationService → TaskProducer, SimulationEngine, ExpectedResultCalculator, StatsCollector
SimulationEngine → Worker/Queue/State/Counter factory
```

Ters bağımlılık oluşturma:

```text
Engine → Controller
State → Service
Model → Spring bean
```

---

# 32. Test Stratejisi

## 32.1 Unit test

- Model,
- State,
- Counter,
- Producer,
- Expected calculator,
- Invariant,
- Worker,
- Engine utility.

## 32.2 Integration test

- Controller,
- Validation,
- Exception mapping,
- Service integration,
- Endpoint lifecycle.

## 32.3 Concurrency test kuralları

- Latch/barrier kullan.
- Timeout koy.
- Executor kapat.
- Test sırasına güvenme.
- Sleep'i senkronizasyon aracı olarak kullanma.
- Unsafe yanlış olmalı assertion'ı kullanma.
- Safe invariant kesin assertion olmalı.

## 32.4 Hız

CI'ı yavaşlatmamak için:

- Normal testlerde makul task sayısı,
- Benchmark ayrı manuel kanıt,
- 100.000 task'lık stres testini normal unit suite'e zorunlu koymama.

---

# 33. GitHub İş Akışı

## 33.1 Branch

Örnekler:

```text
feature/1-immutable-models
feature/2-safe-unsafe-coin-state
feature/6-seeded-task-producer
feature/12-simulation-service
test/15-controller-integration
docs/readme-traceability
```

## 33.2 Commit

İyi örnekler:

```text
Add immutable price update task
Add safe coin state with per-coin lock
Add seeded task producer
Add blocking task queue
Add graceful executor shutdown
Add controller integration tests
```

Kötü örnekler:

```text
fix
son
deneme
çalıştı
final2
```

## 33.3 PR açıklaması

Issue'yu Ar-Ge manuel kapatacaksa:

```text
Refs #15
```

kullanılabilir.

Örnek:

```markdown
## Yapılanlar

- Başarılı simulate integration testi eklendi.
- Validation 400 senaryoları eklendi.
- Stats 404 testi eklendi.
- Concurrent simulate 409 testi deterministik latch ile eklendi.

## İlgili Issue

Refs #15

## Test

- `mvn test`
- `mvn clean verify`

## Not

409 testi sleep yerine kontrollü test double kullanır.
```

## 33.4 Review

Her grup üyesi en az bir review yorumu yapmalı.

Önerilen geliştirici rotasyonu:

- İbrahim, Fırat'ın bir PR'ını review eder.
- Fırat, Ahmet'in bir PR'ını review eder.
- Ahmet, İbrahim'in bir PR'ını review eder.
- Cem Bora ve Tolga kabul/kalite review'larını yapar.

Sadece `LGTM` yeterli değildir. Anlamlı örnekler:

```text
Snapshot aynı lock altında mı alınıyor?
InterruptedException sonrasında interrupt flag neden geri yüklenmiyor?
Task listesi safe ve unsafe için yeniden mi üretiliyor?
409 testi sleep nedeniyle flaky olabilir; latch ile deterministik hâle getirilebilir mi?
Bu DTO assignment'taki expectedUpdateCount alanını taşımıyor.
```

---

# 34. Ar-Ge İnceleme ve Issue Kapatma Protokolü

Issue PR merge edilince otomatik kapatılmayabilir. Ar-Ge şu sırayla ilerler:

1. PR linkini kontrol et.
2. CI sonucunu kontrol et.
3. Kabul kriterlerini tek tek kontrol et.
4. Test/kanıt dosyasını kontrol et.
5. Main branch üzerinde doğrula.
6. Gerekirse değişiklik iste.
7. Onay yorumunu yaz.
8. Issue'yu manuel kapat.

## Standart Ar-Ge yorumu

```markdown
## Ar-Ge Doğrulaması

- [ ] İlgili PR issue içinde paylaşılmış.
- [ ] CI başarılı.
- [ ] Review yorumları çözülmüş.
- [ ] Kabul kriterleri karşılanmış.
- [ ] Test veya çalışma kanıtı mevcut.
- [ ] Mimari sınırlar korunmuş.
- [ ] README / teslim raporu etkisi ele alınmış.
- [ ] Main branch üzerinde doğrulandı.
- [ ] Açık hata bulunmuyor.

**Karar:** APPROVED / CHANGES REQUESTED  
**Doğrulayan:** @kullanici  
**Not:**
```

## Ar-Ge'nin özellikle arayacağı anti-pattern'ler

- Unsafe'i yapay bozma,
- Task listesini üç kere üretme,
- Controller içinde executor,
- BlockingQueue kullanmama,
- Her task için thread,
- Global lock,
- Unlock'u finally dışında bırakma,
- Sadece shutdown,
- Running flag'i finally dışında bırakma,
- Mutable state'i response yapma,
- Flaky sleep test,
- Debug log açık benchmark,
- Missing thread names,
- README'de örnek/sahte sonuç.

---

# 35. Definition of Done

Her issue aşağıdaki şartlar tamamlanmadan bitmiş sayılmaz.

## Developer DoD

- [ ] Issue kapsamı karşılandı.
- [ ] Başka issue alanına kontrolsüz girilmedi.
- [ ] Kod derleniyor.
- [ ] İlgili testler yazıldı.
- [ ] `mvn test` başarılı.
- [ ] `mvn clean verify` başarılı.
- [ ] Public contract mevcut mimariyle uyumlu.
- [ ] Exception ve interrupt davranışı doğru.
- [ ] Gereksiz dependency eklenmedi.
- [ ] Branch'ten PR açıldı.
- [ ] PR issue'ya bağlı.
- [ ] En az bir anlamlı review yapıldı.
- [ ] Review yorumları çözüldü.
- [ ] CI başarılı.
- [ ] Dokümantasyon etkisi belirtildi.

## Ar-Ge DoD

- [ ] Kabul kriterleri doğrulandı.
- [ ] PR main'e merge edildi.
- [ ] Main üzerinde smoke test yapıldı.
- [ ] Kanıtlar mevcut.
- [ ] Mimari ihlal yok.
- [ ] Issue manuel kapatıldı.

---

# 36. Yapay Zekâya Verilecek Kısa Kullanım Şablonları

## Geliştirici için

```text
Bu repository'de AI_PROJECT_GUIDE.md dosyasını ana kaynak kabul et.
Ben Ahmet'im ve #15 Add controller integration tests görevini geliştiriyorum.

Önce mevcut repository yapısını ve bağımlı sınıfları incele.
Mimariyi değiştirme, yeni gereksiz sınıf ekleme.
Bana:
1. Ön koşulları,
2. Dokunacağım dosyaları,
3. Test senaryolarını,
4. Adım adım geliştirme planını,
5. Kritik riskleri,
6. Örnek test iskeletini,
7. Çalıştırma komutlarını,
8. PR ve Ar-Ge checklist'ini
ver.
```

## Ar-Ge için

```text
AI_PROJECT_GUIDE.md dosyasını ana kaynak kabul et.
Ben Cem Bora'yım. #8 Implement fixed worker pool and price workers issue'sunun PR'ını inceliyorum.

PR diff'ini şu başlıklarda değerlendir:
- Issue kabul kriterleri,
- Mimari sınırlar,
- Concurrency doğruluğu,
- Interrupt ve shutdown davranışı,
- Test yeterliliği,
- Flaky test riski,
- README etkisi.

Bana önce kritik bulguları, sonra satır bazlı review yorumlarını,
son olarak APPROVED veya CHANGES REQUESTED kararını ver.
```

## Entegrasyon için

```text
AI_PROJECT_GUIDE.md ana kaynak olsun.
#16 Integrate all modules görevindeyiz.

Mevcut kodu inceleyerek duplicate sınıfları, uyumsuz public contract'ları,
bean dependency sorunlarını ve same-task-list kuralı ihlallerini bul.
Kod üretmeden önce entegrasyon sırasını ve risk tablosunu çıkar.
```

---

# 37. Yapay Zekânın Her Issue İçin Son Cevap Kontrolü

Yapay zekâ cevabını göndermeden önce kendine şunları sormalıdır:

- Bu öneri issue kapsamına uygun mu?
- Projenin in-memory kalmasını koruyor mu?
- Same task list kuralını bozuyor mu?
- Producer–Consumer açıkça kalıyor mu?
- Safe/unsafe ayrımı korunuyor mu?
- Coin başına lock korunuyor mu?
- Tamamlanma ve shutdown doğru mu?
- API sözleşmesini bozuyor mu?
- Testler deterministik mi?
- Başka geliştiricinin sınıfını gereksiz yeniden yazıyor muyum?
- README ve Ar-Ge kanıt ihtiyacını söyledim mi?
- Kullanıcı bu planla doğrudan branch açıp ilerleyebilir mi?

Bu sorulardan biri olumsuzsa cevap revize edilmelidir.

---

# 38. Projenin Başarı Ölçütü

Bu projenin başarılı sayılması için yalnızca endpoint'lerin çalışması yeterli değildir.

Başarı:

```text
Çalışan API
+
Açık Producer–Consumer
+
Doğru fixed worker pool
+
Gerçek unsafe race condition
+
ReentrantLock ile safe compound state
+
AtomicLong ile safe counter
+
Invariant ile matematiksel doğruluk
+
Graceful shutdown
+
Stabil testler
+
Thread dump ve benchmark kanıtı
+
İzlenebilir GitHub ekip çalışması
+
Eksiksiz README ve teslim raporu
```

kombinasyonudur.

---

# 39. Son Not

Bu dosyanın amacı yapay zekânın bütün projeyi tek başına yazması değildir.

Amaç:

- Ekip üyelerinin kendi issue'larını anlayarak geliştirmesi,
- Yapay zekânın mimariyi bozacak rastgele öneriler vermemesi,
- Bütün modüllerin aynı sözleşmeye göre ilerlemesi,
- Ar-Ge incelemesinin ölçülebilir olması,
- Teslimde hiçbir zorunlu maddenin unutulmamasıdır.

Bir issue'nun çözümü sırasında bu dosyayla gerçek repository arasında fark varsa:

1. Mevcut kod incelenir.
2. Fark açıkça belirtilir.
3. Ekip kararı alınır.
4. Gerekirse bu dosya güncellenir.
5. Sessizce ikinci ve çakışan bir mimari oluşturulmaz.
