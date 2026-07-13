# Issue ve Ekip Çalışma Planı

Bu dosya, bir haftalık geliştirme sürecinde issue → branch → PR → review akışını ve bağımlılıkları gösterir. Issue numaraları GitHub'da oluşturulduktan sonra tablo numaraları gerçek issue linkleriyle güncellenmelidir.

## 1. Ekip Rolleri

| Kod | Rol | Ana sorumluluk |
|---|---|---|
| A | Coin & State | Safe/unsafe coin state, snapshot, ReentrantLock |
| B | Producer & Worker Pool | Immutable task, seed producer, queue, worker |
| C | Counter & Invariant | Safe/unsafe counter, expected, invariant |
| D | API & Swagger | Controller, DTO, validation, exceptions, OpenAPI |
| E | Engine & Entegrasyon | Engine, service, stats, shutdown, thread dump, Git koordinasyonu |

İsimler kesinleşince A–E yerine gerçek adlar yazılacaktır.

## 2. Ana Issue Listesi

| Sıra | Issue başlığı | Owner | Reviewer | Branch | Bağımlılık |
|---:|---|---|---|---|---|
| 1 | Bootstrap repository and basic GitHub rules | E | A | `chore/bootstrap-project` | Yok |
| 2 | Add immutable task and snapshot models | A | B | `feature/task-and-snapshot-models` | #1 |
| 3 | Implement safe and unsafe coin state | A | B | `feature/coin-state` | #2 |
| 4 | Implement safe and unsafe processed counters | C | D | `feature/processed-counters` | #1 |
| 5 | Add deterministic task producer and bounded task queue | B | C | `feature/task-producer-queue` | #2 |
| 6 | Add price workers and named fixed worker pool | B | C | `feature/worker-pool` | #3, #4, #5 |
| 7 | Add expected result, stats and invariant calculation | C | D | `feature/metrics-invariant` | #2, #3, #4 |
| 8 | Implement simulation engine and graceful shutdown | E | A | `feature/simulation-engine` | #6, #7 |
| 9 | Implement simulation service and concurrency guard | E | A | `feature/simulation-service` | #8 |
| 10 | Add REST API, validation, error responses and Swagger | D | E | `feature/simulation-api` | #7, #9 |
| 11 | Add unit and integration tests | A/C/D | B/E | `test/concurrency-and-api` | #3–#10 |
| 12 | Add benchmark, thread dump and stress evidence | E | D | `test/benchmark-thread-dump` | #8–#11 |
| 13 | Run and document planned merge conflict | D | E | `docs/merge-conflict-demo` | #1 |
| 14 | Complete README and TESLIM_RAPORU | Tüm ekip | E | `docs/final-delivery` | #10–#13 |

## 3. Kabul Kriterleri

### Issue 1 — Bootstrap repository and basic GitHub rules

- [ ] Spring Boot Maven projesi Java 21 ile açılıyor.
- [ ] `application.properties` kullanılıyor; `application.yml` yok.
- [ ] `.gitignore`, `README.md`, `TESLIM_RAPORU.md`, `docs/ISSUE_PLAN.md` kökte/doğru yerde.
- [ ] CI `mvn clean verify` çalıştırıyor.
- [ ] İlk CI sonrasında `main` ruleset aktif.
- [ ] Collaborator'lar eklendi.

### Issue 2 — Immutable task and snapshot models

- [ ] `PriceUpdateTask` immutable `record`.
- [ ] Poison pill gerçek görevlerle karışmayacak biçimde tanımlandı.
- [ ] `CoinSnapshot`, `ExpectedCoinResult`, `CoinComparison`, `SimulationRunResult` immutable.
- [ ] Mutable state API'ye doğrudan verilmiyor.

### Issue 3 — Safe and unsafe coin state

- [ ] Ortak `CoinState` sözleşmesi tanımlandı.
- [ ] `UnsafeCoinState` kilit kullanmadan dört alanı güncelliyor.
- [ ] `SafeCoinState` coin başına `ReentrantLock` kullanıyor.
- [ ] Lock `finally` içinde bırakılıyor.
- [ ] Snapshot safe sürümde lock altında oluşturuluyor.
- [ ] Unit testler mevcut.

### Issue 4 — Safe and unsafe processed counters

- [ ] Ortak `ProcessedCounter` sözleşmesi var.
- [ ] Unsafe sürüm düz `long` + `count++` kullanıyor.
- [ ] Safe sürüm `AtomicLong` kullanıyor.
- [ ] Safe counter concurrency testi geçiyor.

### Issue 5 — Deterministic task producer and task queue

- [ ] Aynı seed + updates aynı görev listesini üretiyor.
- [ ] Coin seçimi ve delta üretimi dokümante.
- [ ] Üretilen liste immutable.
- [ ] `TaskQueue` açık `BlockingQueue` kullanıyor.
- [ ] Queue tipi ve kapasite kararı README ile uyumlu.
- [ ] Worker sayısı kadar poison pill eklenebiliyor.

### Issue 6 — Price workers and named pool

- [ ] `PriceWorker` `Runnable`.
- [ ] Worker `queue.take()` ile busy waiting yapmadan bekliyor.
- [ ] Gerçek görev tamamlanınca counter ve latch doğru güncelleniyor.
- [ ] Poison pill worker'ı durduruyor.
- [ ] Interrupt flag yeniden set ediliyor.
- [ ] Thread isimleri safe/unsafe mode ve sıra numarasını gösteriyor.
- [ ] Her görev için `new Thread()` açılmıyor.

### Issue 7 — Expected result, stats and invariant

- [ ] Expected sonuç tek thread ile görev listesinden hesaplanıyor.
- [ ] Safe/unsafe elapsed time `System.nanoTime()` ile ölçülüyor.
- [ ] Throughput formülü doğru.
- [ ] Fiyat ve update count invariant'ları bulunuyor.
- [ ] Toplam safe processed count kontrol ediliyor.
- [ ] Her coin için expected/unsafe/safe karşılaştırması üretiliyor.

### Issue 8 — Simulation engine and graceful shutdown

- [ ] `workers` kadar fixed pool oluşturuluyor.
- [ ] `CountDownLatch` gerçek task sayısıyla oluşturuluyor.
- [ ] Producer queue'ya tüm görevleri ve poison pill'leri ekliyor.
- [ ] Engine bütün görevlerin bitmesini bekliyor.
- [ ] `shutdown + awaitTermination + shutdownNow` uygulanıyor.
- [ ] Timeout ve interrupt hata akışı yönetiliyor.
- [ ] Unsafe ve safe state birbirinden bağımsız.

### Issue 9 — Simulation service and concurrency guard

- [ ] Görev listesi yalnız bir kez üretiliyor.
- [ ] Expected/unsafe/safe aynı listeyi kullanıyor.
- [ ] `AtomicBoolean.compareAndSet` ile aynı anda yalnız bir simülasyon.
- [ ] `finally` içinde running flag bırakılıyor.
- [ ] Son tamamlanan immutable stats `AtomicReference` ile yayımlanıyor.
- [ ] `/coins` için safe snapshot saklanıyor.

### Issue 10 — API, validation and Swagger

- [ ] `POST /simulate`, `GET /coins`, `GET /stats` çalışıyor.
- [ ] `updates` 1–100.000, `workers` 1–16 validation.
- [ ] Hatalı parametre 400.
- [ ] Sonuç yoksa 404.
- [ ] İkinci simülasyon 409.
- [ ] Hata formatı tutarlı.
- [ ] Swagger'da parametre, başarı/hata cevapları ve örnekler görünür.
- [ ] Swagger adresi README ile uyumlu.

### Issue 11 — Tests

- [ ] Seed repeatability testi.
- [ ] Expected calculator testi.
- [ ] Safe counter concurrency testi.
- [ ] Safe coin snapshot/invariant testi.
- [ ] Validation 400 testi.
- [ ] 404 testi.
- [ ] 409 concurrency testi.
- [ ] En az bir controller integration testi.
- [ ] `mvn clean verify` geçiyor.

### Issue 12 — Benchmark, stress and thread dump

- [ ] Aynı seed/updates ile 1, 2, 4, 8 worker ölçümü.
- [ ] Safe ve unsafe süre/throughput kaydı.
- [ ] Benchmark sırasında DEBUG task logları kapalı.
- [ ] Unsafe race observation birden fazla çalıştırmada raporlandı.
- [ ] Thread dump alındı.
- [ ] Worker sayısı/state/queue wait/contention/deadlock yorumlandı.
- [ ] Çıktılar `docs/benchmark/` ve `docs/thread-dump/` altında.

### Issue 13 — Planned merge conflict

- [ ] İki farklı branch aynı README satırını farklı değiştiriyor.
- [ ] İlk PR merge ediliyor.
- [ ] İkinci branch gerçek conflict üretiyor.
- [ ] Conflict IntelliJ veya terminalde çözülüyor.
- [ ] Çözüm commit/PR linki kaydediliyor.
- [ ] README'de öğrenilenler yazılıyor.

### Issue 14 — Final delivery docs

- [ ] README'deki bütün placeholder'lar dolduruldu.
- [ ] Tasarım kararları gerçek kodla birebir aynı.
- [ ] Race, invariant, benchmark ve thread dump kanıtları eklendi.
- [ ] Katkı tablosunda beş üyenin branch/PR/review linki var.
- [ ] `TESLIM_RAPORU.md` tamamen dolu.
- [ ] Temiz clone ile çalıştırma doğrulandı.

## 4. Review Rotasyonu

| PR sahibi | Reviewer | Beklenen review odağı |
|---|---|---|
| A | B | Mutable state, lock kapsamı, snapshot |
| B | C | Queue, poison pill, worker lifecycle |
| C | D | Formül, invariant, DTO'ya yansıyan alanlar |
| D | E | HTTP durumları, validation, integration |
| E | A | Engine, shutdown, AtomicBoolean/Reference |

Her üye en az bir **anlamlı review yorumu** yapmalıdır. Sadece “LGTM” yazmak yerine örneğin lock'un `finally` içinde bırakılması, task'ın immutable olması, endpoint'in doğru HTTP durumunu dönmesi gibi somut bir nokta incelenmelidir.

## 5. Branch ve Commit Kuralı

```text
feature/<kisa-konu>
test/<kisa-konu>
docs/<kisa-konu>
chore/<kisa-konu>
```

Örnek commitler:

```text
Add immutable price update task
Add bounded blocking task queue
Fix coin race with per-coin ReentrantLock
Add simulation conflict integration test
Document worker thread dump
```

## 6. PR Açıklama Standardı

```markdown
## Yapılan değişiklikler
- ...

## İlgili issue
Closes #<NO>

## Nasıl test edildi?
- `mvn test`
- ...

## Concurrency etkisi
- Paylaşılan veri:
- Kullanılan koruma:
- Alternatif ve neden seçilmedi:

## Kontrol listesi
- [ ] Testler geçiyor
- [ ] README/Swagger gerekiyorsa güncel
- [ ] Mutable state dışarı açılmıyor
- [ ] Review için küçük ve tek amaçlı
```

## 7. Planlı Merge Conflict

Önerilen güvenli alan: README'de geçici bir `Takım sloganı` satırı.

- Branch 1: `docs/conflict-a`
- Branch 2: `docs/conflict-b`
- Conflict dosyası: `README.md`
- Çözüm sahibi: D
- Reviewer: E

Uygulama kodunda bilinçli conflict oluşturulmayacaktır; teslim riski azaltılır.

## 8. Bir Haftalık Takvim

| Gün | Hedef |
|---|---|
| 1 | Repo/bootstrap, görev dağılımı, model ve interface kararları |
| 2 | Coin state, counter, task producer ve queue |
| 3 | Worker pool, engine ve unsafe/safe akış |
| 4 | Expected/invariant/stats, service ve endpointler |
| 5 | Validation, Swagger, unit/integration testler |
| 6 | Benchmark, stres gözlemi, thread dump, merge conflict |
| 7 | Doküman, clean clone testi, son PR/review ve teslim |

## 9. Merge Sırası

Çakışmayı azaltmak için önerilen sıra:

1. Bootstrap
2. Immutable modeller
3. Counter + coin state
4. Producer + queue
5. Worker pool
6. Metrics/invariant
7. Engine
8. Service
9. API
10. Tests
11. Benchmark/thread dump
12. Final docs

Ortak interface değişikliği gerektiğinde bağımlı PR'lar rebase edilmeden önce ekip kanalında duyurulmalıdır.
