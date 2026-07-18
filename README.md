# Eşzamanlı Kripto Fiyat Simülatörü

Java 21 ve Spring Boot ile geliştirilen bu proje; aynı immutable fiyat güncelleme
görevlerini unsafe ve thread-safe akışlarda işleyerek race condition, lost update,
Producer–Consumer, fixed worker pool, `AtomicLong`, `ReentrantLock`,
`CountDownLatch`, poison pill ve graceful shutdown davranışlarını gösterir.

> **Güncel durum**
>
> - İbrahim'in `#1–#5` görevleri tamamlandı.
> - Fırat'ın `#6–#10` görevleri tamamlandı.
> - Ahmet'in `#11–#15` API ve service görevleri geliştirilecek.
> - `#16–#20` ortak entegrasyon ve teslim görevleri final aşamasında yapılacak.
>
> Kod geliştirmesi tamamlanan bir issue için gerçek PR, test ve kanıt linkleri
> teslimden önce `TESLIM_RAPORU.md` dosyasına eklenmelidir.

---

## Dokümantasyon Haritası

Repository'de yalnızca aşağıdaki ana dokümanlar tutulmalıdır:

| Dosya | Amacı |
|---|---|
| `README.md` | Projenin ne yaptığını, mevcut mimariyi, çalıştırmayı ve güncel durumu açıklar. |
| `docs/ISSUE_PLAN.md` | Gerçek `#1–#20` issue dağılımını, bağımlılıkları ve review sürecini tutar. |
| `docs/AI_PROJECT_GUIDE.md` | Yapay zekâ araçlarının mevcut kod sözleşmelerini bozmadan yönlendirme yapmasını sağlar. |
| `TESLIM_RAPORU.md` | Final PR, test, benchmark, race condition, thread dump ve ekip kanıtlarını içerir. |
| `docs/evidence/` | Gerçek benchmark, race observation ve thread dump çıktılarının yeridir. |

Eski veya kopya README dosyaları repository'de tutulmamalıdır.

---

## 1. Projenin Amacı

Bu proje kapsamlı bir kripto para uygulaması değildir. Domain bilinçli olarak
sade tutulmuştur. Amaç, Java concurrency araçlarını görünür ve ölçülebilir
şekilde kullanmaktır.

Başlangıç coin değerleri:

| Coin | Başlangıç fiyatı |
|---|---:|
| BTC | 60.000 |
| ETH | 3.000 |
| SOL | 150 |

Görev modeli:

```java
package com.infina.price_simulator.model;

public record PriceUpdateTask(
        long sequence,
        String coinId,
        long delta
) {
}
```

Aynı görev listesi:

1. Tek thread ile expected sonucu hesaplamak,
2. Unsafe simülasyonu çalıştırmak,
3. Safe simülasyonu çalıştırmak

için kullanılmalıdır.

---

## 2. Teknolojiler

- Java 21
- Spring Boot
- Maven / Maven Wrapper
- Spring Web
- Spring Validation
- JUnit 5
- Spring Boot Test
- Lombok
- Swagger / OpenAPI — Ahmet'in `#14` görevi
- Git / GitHub

### Kapsam dışı

- PostgreSQL
- Hibernate / JPA
- Redis
- Kafka
- Mikroservis
- JWT / kullanıcı yönetimi
- Haricî kripto API'si

Projede database olarak MySQL kullanıyoruz.

---

## 3. Ana Package ve Mevcut Kod

Ana package:

```text
com.infina.price_simulator
```

### İbrahim tarafından tamamlanan sınıflar

```text
model/
├── PriceUpdateTask.java
└── ExpectedValues.java

counter/
├── Counter.java
├── SafeCounter.java
└── UnsafeCounter.java

metrics/
├── ExpectedCalculator.java
└── InvariantChecker.java

state/
├── CoinState.java
├── SafeCoinState.java
└── UnsafeCoinState.java
```

Mevcut sözleşmeler:

```java
public interface Counter {
    void increment();
    long get();
}
```

```java
public abstract class CoinState {
    public abstract void applyDelta(long delta);
}
```

`CoinState` alanlarının getter'ları Lombok `@Getter` ile üretilir.

### Fırat tarafından tamamlanan sınıflar

```text
engine/
├── TaskProducer.java
├── TaskQueue.java
├── SimulationMode.java
├── PriceWorker.java
├── SimulationEngine.java
└── SimulationEngineException.java

util/
└── WorkerThreadFactory.java

model/
├── CoinRunSnapshot.java
└── SimulationRunResult.java
```

> Repository'deki gerçek sınıf adı bu listeden farklıysa README değil, gerçek kod
> esas alınır ve doküman aynı PR içinde güncellenir. Aynı sorumluluk için ikinci
> bir sınıf oluşturulmaz.

### Ahmet tarafından eklenecek alanlar

```text
api/
├── controller/
├── dto/
└── exception/

config/
└── OpenApiConfig.java

service/
└── SimulationService.java
```

---

## 4. Tamamlanan Concurrency Yapısı

### 4.1 Immutable ve seed'li task üretimi

`TaskProducer`:

- Aynı seed ve update sayısı için aynı görev listesini üretir.
- Yalnızca BTC, ETH ve SOL görevleri oluşturur.
- Sequence değerlerini 1'den başlatır.
- Delta değerlerini takım kararı olarak `-100..100`, sıfır hariç üretir.
- Listeyi `List.copyOf()` ile immutable döndürür.

Task listesi yalnızca bir kez üretilmeli ve expected, unsafe ve safe akışlarda
yeniden kullanılmalıdır. Bu kuralın orchestration tarafındaki garantisi Ahmet'in
`SimulationService` görevinde tamamlanacaktır.

### 4.2 BlockingQueue

Producer–Consumer iletişiminde sınırlı kapasiteli `ArrayBlockingQueue` kullanılır.

Bu seçim:

- Kuyruğun kontrolsüz büyümesini engeller.
- Bellek kullanımını sınırlar.
- Queue dolduğunda `put()` üzerinden backpressure sağlar.
- Queue boşken `take()` ile busy waiting yapılmadan beklenmesini sağlar.

`TaskQueue` singleton Spring bean değildir. Her engine run için yeni queue
oluşturulur.

### 4.3 Fixed worker pool

Her task için yeni thread açılmaz.

```java
Executors.newFixedThreadPool(workerCount, workerThreadFactory)
```

Thread isimleri:

```text
safe-worker-1
safe-worker-2
unsafe-worker-1
unsafe-worker-2
```

`PriceWorker` queue'dan task alır, coin state'i günceller, processed counter'ı
artırır ve completion mekanizmasına haber verir.

### 4.4 Safe ve unsafe state

Unsafe counter:

```java
private long value;

@Override
public void increment() {
    value++;
}
```

Safe counter:

```java
private final AtomicLong value = new AtomicLong();

@Override
public void increment() {
    value.incrementAndGet();
}
```

Safe coin state, coin başına `ReentrantLock` kullanır:

```java
@Override
public void applyDelta(long delta) {
    lock.lock();
    try {
        updateState(delta);
    } finally {
        lock.unlock();
    }
}
```

`currentPrice`, `updateCount`, `lastDelta` ve `lastUpdatedBy` aynı kritik
bölümde güncellenir.

### 4.5 Completion ve worker sonlandırma

- `CountDownLatch` gerçek task sayısıyla oluşturulur.
- Her gerçek task başarılı veya hatalı tamamlandığında latch bir kez azaltılır.
- Poison pill latch ve processed count hesabına dahil edilmez.
- Worker sayısı kadar poison pill gönderilir.
- Engine, gerçek task'lar tamamlanmadan sonuç döndürmez.

### 4.6 Graceful shutdown

Executor yaşam döngüsü:

1. `shutdown()`
2. `awaitTermination(...)`
3. Timeout durumunda `shutdownNow()`
4. Zorunlu kapatma sonrası yeniden `awaitTermination(...)`
5. Interrupt durumunda `shutdownNow()`
6. `Thread.currentThread().interrupt()`

Her run için yeni state, counter, queue, latch ve executor oluşturulur.

---

## 5. Expected Sonuç ve Invariant

`ExpectedCalculator`, aynı task listesini tek thread ile işler.

Her coin için:

```text
safeCurrentPrice
=
initialPrice + ilgili coin task'larının delta toplamı
```

```text
safeUpdateCount
=
ilgili coin için üretilen task sayısı
```

Genel sayaç için:

```text
safeProcessedCount
=
submittedUpdates
```

Unsafe sonucun her çalıştırmada yanlış çıkması beklenmez. Race condition
zamanlamaya bağlıdır ve yapay biçimde oluşturulmaz.

---

## 6. Tasarım Kararları

| Karar noktası | Seçim | Gerekçe |
|---|---|---|
| Task modeli | Java `record` | Immutable görev |
| Seed | `Random(seed)` | Tekrarlanabilir iş yükü |
| Task listesi | `List.copyOf()` | Aşamalar arasında değişikliği engellemek |
| Queue | Sınırlı `ArrayBlockingQueue` | Backpressure ve bellek kontrolü |
| Worker pool | Fixed thread pool | Thread sayısını sınırlandırmak |
| Worker isimleri | `safe-worker-N`, `unsafe-worker-N` | Log ve thread dump okunabilirliği |
| Unsafe counter | `long` + `value++` | Race condition gözlemi |
| Safe counter | `AtomicLong` | Atomik increment |
| Safe coin state | Coin başına `ReentrantLock` | Compound state'i tutarlı güncellemek |
| Completion | `CountDownLatch` | Gerçek task'ların tamamını beklemek |
| Worker bitişi | Poison pill | Queue'da bekleyen worker'ları sonlandırmak |
| Shutdown | `shutdown/awaitTermination/shutdownNow` | Timeout ve interrupt kontrollü kapanma |
| Süre ölçümü | `System.nanoTime()` | Monotonic süre ölçümü |
| Throughput | Submitted task / elapsed second | Unsafe counter kaybından bağımsız ölçüm |

Ahmet'in görevleri tamamlandığında aşağıdaki iki karar da bu tabloya
gerçek implementasyonla eklenecektir:

- Sonucun güvenli yayımlanması,
- İkinci eşzamanlı simülasyon isteğinin engellenmesi.

---

## 7. Testler

Tüm testler:

```powershell
.\mvnw.cmd test
```

Temiz doğrulama:

```powershell
.\mvnw.cmd clean verify
```

Producer ve queue:

```powershell
.\mvnw.cmd "-Dtest=TaskProducerTest,TaskQueueTest" test
```

Worker ve engine:

```powershell
.\mvnw.cmd "-Dtest=WorkerThreadFactoryTest,PriceWorkerTest,SimulationEngineTest" test
```

PowerShell'de virgül içeren `-Dtest` argümanı tırnak içinde yazılmalıdır.

Zorunlu test kapsamı:

- Aynı seed → aynı task listesi,
- Safe counter concurrency,
- Safe coin state,
- Expected hesap,
- Invariant,
- Queue FIFO ve blocking,
- Backpressure,
- Poison pill,
- Worker isimleri,
- Completion,
- Graceful shutdown,
- Worker thread leak kontrolü.

Unsafe implementasyonun her testte yanlış çıkmasını bekleyen flaky assertion
yazılmaz.

---

## 8. Benchmark ve Thread Dump

Fırat'ın `#10` görevi benchmark ve thread dump altyapısını içerir. Final teslimde
örnek veya uydurma sayı kullanılmamalıdır; gerçek çıktılar aşağıdaki dosyalara
eklenmelidir:

```text
docs/evidence/benchmark-results.md
docs/evidence/race-observation.md
docs/evidence/thread-dump-analysis.md
docs/evidence/thread-dump.txt
```

Benchmark:

```powershell
.\mvnw.cmd "-Dbenchmark=true" "-Dtest=SimulationBenchmarkTest" test
```

Thread dump evidence:

```powershell
.\mvnw.cmd "-DthreadDumpDemo=true" "-Dtest=ThreadDumpEvidenceTest" test
```

PID alındıktan sonra:

```powershell
jcmd <PID> Thread.print > docs\evidence\thread-dump.txt
```

Karşılaştırılacak worker sayıları:

```text
1, 2, 4, 8
```

Task bazlı DEBUG logları benchmark sırasında kapalı olmalıdır.

---

## 9. Uygulama Ayarları

Repository'de yalnızca:

```text
src/main/resources/application.properties
```

bulunmalıdır. `application.yml` silinmelidir.

```properties
spring.application.name=price-simulator
server.port=8080

springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html

logging.level.root=INFO
logging.level.com.infina.price_simulator=INFO
logging.level.com.infina.price_simulator.engine.PriceWorker=INFO
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n
```

---

## 10. Çalıştırma

Java sürümü:

```powershell
java -version
```

Test:

```powershell
.\mvnw.cmd clean verify
```

Uygulamayı başlatma:

```powershell
.\mvnw.cmd spring-boot:run
```

Ahmet'in API görevleri tamamlanmadan uygulama ayağa kalkabilir ancak zorunlu
endpoint'lerin mevcut olması beklenmez.

---

## 11. API

`SimulationController`, gerçek `SimulationService`'e bağlı ve doğrulandı:

```http
POST /simulate?updates=500&workers=4&seed=42
GET /coins
GET /stats
```

Örnek `POST /simulate` cevabı (gerçek çalıştırmadan alınmıştır):

```json
{
  "seed": 42, "submittedUpdates": 500,
  "unsafeProcessedUpdates": 495, "safeProcessedUpdates": 500,
  "workers": 4, "unsafeElapsedMs": 3, "safeElapsedMs": 2,
  "unsafeThroughputPerSec": 149490, "safeThroughputPerSec": 226911,
  "safeInvariantPassed": true,
  "coins": [
    { "id": "BTC", "initial": 60000, "expected": 59454, "unsafe": 59521, "safe": 59454,
      "expectedUpdateCount": 168, "unsafeUpdateCount": 167, "safeUpdateCount": 168 }
  ]
}
```

Validation:

| Parametre | Kural |
|---|---|
| `updates` | 1–100.000 |
| `workers` | 1–16 |
| `seed` | Opsiyonel — verilmezse `System.nanoTime()` ile üretilir, cevaptaki `seed` alanından okunabilir |

HTTP durumları (`GlobalExceptionHandler` ile doğrulandı):

| Durum | HTTP |
|---|---:|
| Geçersiz parametre (`@Min`/`@Max` ihlali, `ConstraintViolationException`) | 400 |
| Eksik parametre (`MissingServletRequestParameterException`) | 400 |
| Sonuç bulunamadı (`/stats`, `/coins` — henüz simülasyon yok) | 404 |
| Başka simülasyon çalışıyor (`AtomicBoolean` ile tespit) | 409 |
| Beklenmeyen hata | 500 |

Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON:

```text
http://localhost:8080/api-docs
```

`config/OpenApiConfig.java` API başlığı ve açıklamasını tanımlar. Not:
`springdoc-openapi-starter-webmvc-ui` önce `2.5.0` idi; Spring Boot `4.1.0`
(Spring Framework 7) ile `ControllerAdviceBean` reflection uyumsuzluğu
(`NoSuchMethodError`) verdiği için `3.0.3`'e yükseltildi — bu sürüm
Spring Boot 4 hattını hedefliyor.

Controller integration testi: `SimulationControllerIntegrationTest`
(`src/test/.../api/controller`) — 400/200 senaryolarını ve `/simulate` →
`/stats` → `/coins` akışını `MockMvc` ile uçtan uca doğrular.

---

## 12. Issue Durumu

| Issue | Sorumlu | Durum |
|---|---|---|
| #1–#5 | İbrahim | ✅ Kod ve test geliştirmesi tamamlandı |
| #6–#10 | Fırat | ✅ Kod ve teknik kanıt altyapısı tamamlandı |
| #11–#15 | Ahmet | ⏳ Geliştirilecek |
| #16–#20 | Ortak | ⏳ Final entegrasyon ve teslim |

`#10` için gerçek benchmark ve thread dump dosyalarının final kontrolde dolu
olması gerekir. Altyapının yazılmış olması tek başına final kanıt yerine geçmez.

---

## 13. Ekip

| Üye | Sorumluluk |
|---|---|
| İbrahim | Model, state, counter, expected, invariant ve core testler |
| Fırat | Producer, queue, worker pool, engine lifecycle ve evidence altyapısı |
| Ahmet | API DTO, exception, service, endpoint, validation, Swagger ve integration test |
| Cem Bora | Ar-Ge review, requirements traceability ve README kontrolü |
| Tolga | Ar-Ge review, teslim raporu ve final doğrulama |

PR ve review linkleri `TESLIM_RAPORU.md` dosyasına eklenecektir.

---

## 14. Kalan Çalışmalar

### Ahmet

- [x] API DTO'ları ve exception handler,
- [x] `SimulationService`,
- [x] Task listesinin yalnızca bir kez üretilmesi,
- [x] Expected → unsafe → safe orchestration,
- [x] `AtomicBoolean` ile 409 kontrolü,
- [x] Son immutable result'ın güvenli paylaşılması,
- [x] `/simulate`, `/coins`, `/stats`,
- [x] Validation (400/404/409 gerçek çalıştırmayla doğrulandı),
- [x] Swagger/OpenAPI (`OpenApiConfig`, springdoc `3.0.3`'e yükseltildi),
- [x] Controller integration testleri (`SimulationControllerIntegrationTest`).

### Ortak

- [ ] Modül entegrasyonu,
- [ ] Gerçek race observation kayıtları,
- [ ] Gerçek benchmark değerleri,
- [ ] Thread dump analizi,
- [ ] Bilinçli merge conflict,
- [ ] README ve teslim raporu final kontrolü,
- [ ] PR/review linkleri,
- [ ] Clean-clone testi,
- [ ] Final `mvn clean verify`.

---

## 15. Teslim Öncesi İlke

README bir geliştirme günlüğü değildir. Yalnızca gerçek implementasyonu ve
doğrulanmış kararları anlatmalıdır. Henüz yazılmamış özellikler “planlanan”
olarak belirtilmeli; örnek metrikler gerçek sonuç gibi sunulmamalıdır.
