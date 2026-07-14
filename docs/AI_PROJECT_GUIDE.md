# AI Project Guide

Bu dosya, projeyi bir yapay zekâ aracına verirken kullanılacak teknik bağlamdır.
Yapay zekâ önerileri mevcut repository kodundan üstün değildir.

## 1. Değiştirilemez Gerçekler

- Java 21 ve Spring Boot kullanılır.
- Ana package: `com.infina.price_simulator`
- Uygulama adı: `price-simulator`
- `application.properties` kullanılır; `application.yml` kullanılmaz.
- Proje in-memory çalışır; JPA/veritabanı eklenmez.
- Expected, unsafe ve safe aynı immutable task listesini kullanır.
- Açık `BlockingQueue<PriceUpdateTask>` bulunur.
- Her task için thread açılmaz.
- Fixed worker pool kullanılır.
- Unsafe sonuç yapay olarak bozulmaz.
- Safe counter `AtomicLong` kullanır.
- Safe coin state coin başına `ReentrantLock` kullanır.
- `unlock()` `finally` içindedir.
- Gerçek task'ların tamamı beklenir.
- Poison pill gerçek task değildir.
- Graceful shutdown timeout ve interrupt ile yönetilir.
- Task bazlı DEBUG log benchmark sırasında kapalıdır.

## 2. Mevcut Kod Sözleşmeleri

Yapay zekâ aşağıdaki sınıfları yeniden adlandırmamalı veya ikinci kopyalarını
oluşturmamalıdır.

```java
package com.infina.price_simulator.model;

public record PriceUpdateTask(
        long sequence,
        String coinId,
        long delta
) {
}
```

```java
package com.infina.price_simulator.counter;

public interface Counter {
    void increment();
    long get();
}
```

```java
package com.infina.price_simulator.state;

public abstract class CoinState {
    public abstract void applyDelta(long delta);
}
```

`CoinState`, Lombok getter'larıyla aşağıdaki alanları sunar:

```text
id
initialPrice
currentPrice
updateCount
lastDelta
lastUpdatedBy
```

Metrics sözleşmeleri:

```text
ExpectedCalculator.calculate(List<CoinState>, List<PriceUpdateTask>)
InvariantChecker.check(List<CoinState>, Map<String, ExpectedValues>)
```

Fırat'ın engine tarafındaki ana sınıfları:

```text
TaskProducer
TaskQueue
SimulationMode
PriceWorker
SimulationEngine
SimulationEngineException
WorkerThreadFactory
CoinRunSnapshot
SimulationRunResult
```

Repository'de gerçek imza farklıysa önce kod okunur, tahminle ikinci API yazılmaz.

## 3. Katman Sınırları

```text
Controller -> SimulationService
SimulationService -> TaskProducer, ExpectedCalculator, SimulationEngine, InvariantChecker
SimulationEngine -> TaskQueue, PriceWorker, CoinState, Counter
```

Yasak bağımlılıklar:

```text
Engine -> Controller
State -> Service
Model -> Spring Web
Controller -> ExecutorService / BlockingQueue / ReentrantLock
```

Mutable `CoinState` API'ye doğrudan dönülmez. API immutable DTO veya
`SimulationRunResult` içinden oluşturulan immutable response kullanır.

## 4. Issue Sahipliği ve Durum

| Issue | Sahip | Durum |
|---|---|---|
| #1–#5 | İbrahim | Tamamlandı |
| #6–#10 | Fırat | Tamamlandı |
| #11–#15 | Ahmet | Geliştirilecek |
| #16–#20 | Ortak | Final aşama |

Ar-Ge:

- Cem Bora
- Tolga

## 5. Yapay Zekâdan İstenen Cevap Formatı

Bir kullanıcı örneğin:

```text
#15 Add controller integration tests görevini geliştiriyorum
```

dediğinde cevap şu sırada olmalıdır:

1. Görevin amacı,
2. Ön koşullar ve bağımlı sınıflar,
3. Mevcut kodda incelenecek dosyalar,
4. Değiştirilecek/eklenecek dosyalar,
5. Adım adım geliştirme planı,
6. Test senaryoları,
7. Concurrency ve mimari riskler,
8. Lokal Maven komutları,
9. PR açıklaması,
10. Developer Definition of Done,
11. Ar-Ge review checklist,
12. Bu issue kapsamında yapılmaması gerekenler.

Repository görülmeden public metot imzası kesinmiş gibi üretilmemelidir.

## 6. Issue Kılavuzu

### #1 — Immutable task and result models

Sahip: İbrahim  
Durum: Tamamlandı

Kontrol:

- Immutable `record`,
- Setter yok,
- Mutable koleksiyon sızmıyor,
- Aynı sorumluluk için duplicate model yok.

### #2 — Safe and unsafe coin states

Sahip: İbrahim  
Durum: Tamamlandı

Kontrol:

- Unsafe lock kullanmaz.
- Safe coin başına `ReentrantLock`.
- İlişkili alanlar aynı kritik bölümde.
- `unlock()` `finally` içinde.
- Unsafe yapay hata üretmez.

### #3 — Safe and unsafe counters

Sahip: İbrahim  
Durum: Tamamlandı

Kontrol:

- Unsafe `long` increment.
- Safe `AtomicLong`.
- Counter coin state sorumluluğu taşımaz.

### #4 — Expected and invariant

Sahip: İbrahim  
Durum: Tamamlandı

Kontrol:

- Expected tek thread.
- Fiyat ve update count ayrı doğrulanır.
- Aynı task listesi kullanılır.

### #5 — Core tests

Sahip: İbrahim  
Durum: Tamamlandı

Kontrol:

- Safe concurrency deterministik test edilir.
- Unsafe yanlış olmak zorunda değildir.
- Executor'lar test sonunda kapatılır.

### #6 — Seeded task producer

Sahip: Fırat  
Durum: Tamamlandı

Kontrol:

- Aynı seed aynı liste.
- Sequence ve coin kuralları.
- Immutable list.
- Producer state/queue yönetmez.

### #7 — Blocking task queue

Sahip: Fırat  
Durum: Tamamlandı

Kontrol:

- Açık `BlockingQueue`.
- Bounded queue ve backpressure.
- Busy waiting yok.
- Poison pill gerçek task değildir.
- Her run yeni queue.

### #8 — Fixed pool and workers

Sahip: Fırat  
Durum: Tamamlandı

Kontrol:

- Fixed pool.
- Her task için thread yok.
- Worker `take()` kullanır.
- Anlamlı thread isimleri.
- Interrupt flag korunur.
- Worker failure latch'i sonsuza kadar bekletmez.

### #9 — Completion and graceful shutdown

Sahip: Fırat  
Durum: Tamamlandı

Kontrol:

- Latch gerçek task sayısıyla.
- Poison pill latch/counter dışı.
- `shutdown`, `awaitTermination`, `shutdownNow`.
- Timeout ve interrupt.
- Run sonunda worker leak yok.

### #10 — Benchmark and thread dump

Sahip: Fırat  
Durum: Kod/evidence altyapısı tamamlandı

Kontrol:

- 1/2/4/8 worker.
- Aynı seed ve task listesi.
- DEBUG kapalı.
- Gerçek çıktı dosyaları.
- Thread isimleri ve state yorumu.
- Deadlock kontrolü.

### #11 — API DTOs and exception handling

Sahip: Ahmet

Yapılacaklar:

- Mevcut result modellerini incele.
- Immutable DTO'lar oluştur.
- `CoinState` doğrudan serialize edilmez.
- 400/404/409/500 için standart `ErrorResponse`.
- Stack trace client'a gönderilmez.
- DTO isimleri mevcut modelle çakışmaz.

Test:

- Mapping,
- Error response,
- Validation body.

### #12 — Simulation orchestration service

Sahip: Ahmet

Zorunlu akış:

```text
AtomicBoolean guard
→ seed belirleme
→ taskProducer.generate yalnız bir kez
→ ExpectedCalculator
→ SimulationEngine UNSAFE
→ SimulationEngine SAFE
→ invariant/stats
→ latest immutable result
→ finally running=false
```

Kurallar:

- Service HTTP status bilmez.
- Engine details controller'a taşınmaz.
- Same task list referansı/içeriği korunur.
- Latest result `AtomicReference` veya eşdeğer güvenli yayınla saklanır.
- Sonuç yoksa domain/application exception.

Test:

- Producer bir kez çağrılır.
- Aynı listeler engine'e gider.
- İkinci çağrı reddedilir.
- Exception sonrası running flag bırakılır.
- Latest result saklanır.

### #13 — Endpoints

Sahip: Ahmet

```http
POST /simulate
GET /coins
GET /stats
```

Kurallar:

- Controller ince.
- Queue/executor/lock controller içinde yok.
- `/simulate` tamamlanmış sonuç döner.
- `/coins` safe snapshot/DTO döner.
- `/stats` son tamamlanan sonucu döner.

### #14 — Swagger and validation

Sahip: Ahmet

```text
updates: 1–100000
workers: 1–16
seed: optional
```

Properties ile uyumlu adresler:

```text
/swagger-ui.html
/api-docs
```

Test:

- updates 0 ve 100001 → 400
- workers 0 ve 17 → 400
- Swagger erişimi

### #15 — Controller integration tests

Sahip: Ahmet

Zorunlu senaryolar:

- Başarılı simulate → 200,
- Geçersiz parametre → 400,
- Sonuç yok → 404,
- Aynı anda ikinci simulate → 409,
- Simülasyon sonrası stats/coins → 200,
- Safe invariant response içinde doğrulanır.

409 testi rastgele `Thread.sleep()` varsayımına dayanmamalıdır. Kontrollü latch,
test double veya deterministik service davranışı kullanılmalıdır.

Testler:

- Spring context kullanmalı,
- Sıra bağımsız olmalı,
- Latest result/running state sızdırmamalı,
- Worker leak bırakmamalı.

### #16 — Integrate all modules

Sahip: Ortak

Kontrol:

- Duplicate sınıf yok.
- Package `com.infina.price_simulator`.
- Same-task-list kuralı.
- Spring bean bağımlılıkları.
- `mvn clean verify`.
- Swagger smoke test.

### #17 — Race observation

Sahip: Ortak, teknik lider Fırat

Kontrol:

- Unsafe yapay bozulmaz.
- Birden fazla run.
- Parametreler kayıtlı.
- Safe invariant her run kontrol edilir.
- Gerçek değerler evidence dosyasına yazılır.

### #18 — Intentional merge conflict

Sahip: Ortak

Kontrollü doküman dosyasında gerçek conflict oluşturulur. Production kodu
kullanılmaz. Branch, PR ve çözüm commit'i raporlanır.

### #19 — README and delivery report

Sahip: Ortak  
Ar-Ge liderleri: Cem Bora ve Tolga

Kontrol:

- Yanlış package/path yok.
- Sahte benchmark değeri yok.
- Placeholder final teslimde yok.
- PR/review linkleri gerçek.
- README ve TESLIM_RAPORU çelişmiyor.

### #20 — Final regression and clean clone

Sahip: Ortak

```powershell
git clone <repo>
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Kontrol:

- Uygulama açılır.
- Swagger açılır.
- 200/400/404/409 çalışır.
- Safe invariant başarılı.
- `target`, `.idea`, log veya secret commit edilmemiştir.

## 7. Yapay Zekâ Anti-Pattern Kontrolü

Yapay zekâ şu önerileri vermemelidir:

- Yeni package kökü,
- JPA/veritabanı,
- Task listesini safe ve unsafe için yeniden üretmek,
- Controller içinde executor,
- Executor'ın iç kuyruğunu tek queue olarak kullanmak,
- Her task için `new Thread()`,
- Global coin lock,
- `unlock()` işlemini `finally` dışında yapmak,
- Yalnızca `shutdown()` kullanmak,
- Running flag'i `finally` dışında bırakmak,
- Unsafe sonucu manuel bozmak,
- Flaky sleep test,
- Mutable `CoinState` response,
- Sahte benchmark sonucu.

## 8. Geliştirici Prompt Şablonu

```text
docs/AI_PROJECT_GUIDE.md dosyasını ana kaynak kabul et.
Ben <isim> ve #<no> görevini geliştiriyorum.

Önce repository'deki mevcut sınıfları ve metot imzalarını incele.
Yeni veya duplicate mimari oluşturma.

Bana:
1. Ön koşulları,
2. Dokunulacak dosyaları,
3. Adım adım planı,
4. Testleri,
5. Riskleri,
6. Maven komutlarını,
7. PR açıklamasını,
8. Developer DoD ve Ar-Ge checklist'ini
ver.
```

## 9. Ar-Ge Prompt Şablonu

```text
docs/AI_PROJECT_GUIDE.md dosyasını ana kaynak kabul et.
Ben Cem Bora/Tolga ve #<no> issue PR'ını inceliyorum.

PR diff'ini:
- kabul kriterleri,
- mevcut kod sözleşmeleri,
- concurrency doğruluğu,
- test stabilitesi,
- mimari sınırlar,
- dokümantasyon etkisi

açısından değerlendir. Kritik bulguları önce yaz ve sonunda
APPROVED veya CHANGES REQUESTED kararı ver.
```
