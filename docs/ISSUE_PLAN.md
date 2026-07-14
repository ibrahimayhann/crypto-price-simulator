# Issue ve Ekip Çalışma Planı

Bu dosya repository'deki gerçek `#1–#20` issue yapısının tek kaynağıdır.
Eski A–E rol tablosu ve 14 issue'luk plan kullanılmaz.

## 1. Ekip

| Üye | Rol |
|---|---|
| İbrahim | Geliştirici — model, state, counter, metrics |
| Fırat | Geliştirici — producer, queue, worker, engine lifecycle, evidence |
| Ahmet | Geliştirici — API, service, validation, Swagger, integration |
| Cem Bora | Ar-Ge — review, requirements traceability, issue doğrulama |
| Tolga | Ar-Ge — review, teslim raporu, final doğrulama |

## 2. Issue Listesi

| No | Başlık | Sorumlu | Ar-Ge reviewer | Durum |
|---:|---|---|---|---|
| #1 | Add immutable task and result models | İbrahim | Cem Bora | ✅ |
| #2 | Implement safe and unsafe coin states | İbrahim | Tolga | ✅ |
| #3 | Implement safe and unsafe counters | İbrahim | Cem Bora | ✅ |
| #4 | Add expected calculation and invariant checks | İbrahim | Tolga | ✅ |
| #5 | Add core state and metrics unit tests | İbrahim | Cem Bora | ✅ |
| #6 | Implement seeded task producer | Fırat | Tolga | ✅ |
| #7 | Implement blocking task queue | Fırat | Cem Bora | ✅ |
| #8 | Implement fixed worker pool and price workers | Fırat | Tolga | ✅ |
| #9 | Add completion and graceful shutdown mechanisms | Fırat | Cem Bora | ✅ |
| #10 | Add benchmark and thread dump evidence | Fırat | Tolga | ✅ altyapı; final gerçek kanıt kontrolü gerekli |
| #11 | Add API DTOs and exception handling | Ahmet | Cem Bora | ⏳ |
| #12 | Implement simulation orchestration service | Ahmet | Tolga | ⏳ |
| #13 | Add simulate, coins and stats endpoints | Ahmet | Cem Bora | ⏳ |
| #14 | Add Swagger and request validation | Ahmet | Tolga | ⏳ |
| #15 | Add controller integration tests | Ahmet | Cem Bora | ⏳ |
| #16 | Integrate all modules | Ortak | Cem Bora + Tolga | ⏳ |
| #17 | Perform race condition observation runs | Ortak, teknik lider Fırat | Tolga | ⏳ |
| #18 | Resolve intentional merge conflict | Ortak | Cem Bora | ⏳ |
| #19 | Complete README and delivery report | Ortak | Cem Bora + Tolga | 🔄 |
| #20 | Final regression and clean-clone test | Ortak | Cem Bora + Tolga | ⏳ |

## 3. Bağımlılık Sırası

```text
#1 → #2, #3
#2 + #3 → #4
#1–#4 → #5

#1 → #6 → #7
#2 + #3 + #7 → #8
#8 → #9
#9 → #10

#1 + #4 + #9 → #11, #12
#11 + #12 → #13
#13 → #14 → #15

#1–#15 → #16
#16 → #17
#18 bağımsız olarak kontrollü doküman dosyasında yapılabilir
#16–#18 → #19
#19 → #20
```

## 4. Tamamlanan Issue Kabul Özeti

### #1 — Immutable modeller

- `PriceUpdateTask` immutable `record`.
- Sonuç modelleri mutable state'i dışarı sızdırmaz.
- Koleksiyonlar gerekiyorsa defensive copy kullanır.

### #2 — Safe ve unsafe coin state

- Unsafe state lock kullanmaz.
- Safe state coin başına `ReentrantLock` kullanır.
- `unlock()` `finally` içindedir.
- İlişkili state alanları aynı kritik bölümde güncellenir.

### #3 — Safe ve unsafe counters

- `Counter` ortak sözleşmesi vardır.
- Unsafe counter düz `long` ve increment kullanır.
- Safe counter `AtomicLong` kullanır.

### #4 — Expected ve invariant

- Expected hesap aynı task listesini tek thread ile işler.
- Fiyat ve update count ayrı kontrol edilir.
- Safe processed count doğrulanır.

### #5 — Core unit testleri

- Seed dışındaki model/state/counter/metrics davranışları test edilir.
- Unsafe'in her çalışmada yanlış olmasını bekleyen flaky test yoktur.

### #6 — Seeded task producer

- Aynı seed + updates aynı listeyi üretir.
- Task listesi immutable döner.
- Coin, sequence ve delta kuralları test edilir.

### #7 — Blocking task queue

- Açık `BlockingQueue<PriceUpdateTask>` kullanılır.
- Sınırlı `ArrayBlockingQueue` tercih edilir.
- `put/take`, FIFO, blocking ve backpressure test edilir.
- Worker sayısı kadar poison pill desteklenir.

### #8 — Fixed worker pool ve worker

- Her task için thread açılmaz.
- Fixed thread pool kullanılır.
- Worker isimleri anlamlıdır.
- Worker queue'dan `take()` ile görev alır.
- Interrupt flag korunur.

### #9 — Completion ve graceful shutdown

- Gerçek task sayısı kadar `CountDownLatch`.
- Poison pill gerçek task sayılmaz.
- Engine bütün task'ların bitmesini bekler.
- `shutdown`, `awaitTermination`, `shutdownNow` uygulanır.
- Timeout ve interrupt akışları yönetilir.

### #10 — Benchmark ve thread dump

- 1/2/4/8 worker ölçüm altyapısı vardır.
- Task DEBUG logları benchmark sırasında kapalıdır.
- Thread dump worker isimlerini görünür kılar.
- Final issue kapatılmadan gerçek çıktı dosyaları kontrol edilir.

## 5. Ahmet'in Issue Kabul Kriterleri

### #11 — API DTO ve exception handling

- Mutable `CoinState` doğrudan response olmaz.
- DTO'lar immutable tasarlanır.
- 400, 404, 409, 500 standart hata formatına çevrilir.
- İç stack trace client'a gönderilmez.

### #12 — Simulation orchestration service

- Task listesi yalnızca bir kez üretilir.
- Expected, unsafe ve safe aynı listeyi kullanır.
- `AtomicBoolean.compareAndSet` ile aynı anda tek simülasyon çalışır.
- Running flag `finally` içinde bırakılır.
- Son immutable result güvenli yayımlanır.
- Service HTTP ayrıntılarını bilmez.

### #13 — Endpoint'ler

```http
POST /simulate
GET /coins
GET /stats
```

- Controller ince kalır.
- Queue, executor ve lock controller içinde yönetilmez.
- `/simulate` bütün işler bitince döner.

### #14 — Swagger ve validation

- `updates`: 1–100.000
- `workers`: 1–16
- `seed`: opsiyonel
- Swagger adresi README ve properties ile aynıdır.
- 400/404/409/500 cevapları dokümante edilir.

### #15 — Controller integration testleri

- Spring context kullanılan gerçek integration test.
- 200, 400, 404 ve 409 test edilir.
- 409 testi `Thread.sleep()` varsayımına dayanmaz.
- Testler sıra bağımsızdır.
- Testler worker/executor sızıntısı bırakmaz.

## 6. Ortak Issue Kabul Kriterleri

### #16 — Entegrasyon

- Duplicate model veya ikinci mimari oluşturulmaz.
- `com.infina.price_simulator` package yapısı korunur.
- Same-task-list kuralı doğrulanır.
- Tüm modüller `mvn clean verify` ile geçer.

### #17 — Race observation

- Aynı seed ve parametrelerle birden fazla run yapılır.
- Unsafe sonuç yapay olarak bozulmaz.
- Bozulan run sayısı ve safe invariant kaydedilir.
- Gerçek sonuç `docs/evidence/race-observation.md` dosyasına yazılır.

### #18 — Merge conflict

- İki branch aynı dosyanın aynı satırını değiştirir.
- Gerçek Git conflict oluşur.
- Manuel çözülür.
- Commit ve PR linkleri raporlanır.
- Production kodu risk için kullanılmaz.

### #19 — Doküman

- README gerçek implementasyonla uyumludur.
- TESLIM_RAPORU gerçek link ve metrikleri içerir.
- Placeholder'lar final teslimde kalmaz.
- Çelişkili Swagger, package veya sınıf adı bulunmaz.

### #20 — Final regression

- Temiz clone.
- `mvn clean verify`.
- Uygulama açılışı.
- Swagger ve endpoint doğrulaması.
- 400/404/409.
- Safe invariant.
- Repository hijyeni.

## 7. Branch ve PR Standardı

Önerilen branch formatı:

```text
feature/<issue-no>-<short-name>
test/<issue-no>-<short-name>
docs/<issue-no>-<short-name>
```

PR issue'yu Ar-Ge manuel kapatacaksa:

```text
Refs #<issue-no>
```

kullanılır. `Closes #...` kullanılması issue'nun merge ile otomatik kapanmasına
neden olabilir.

PR açıklaması:

```markdown
## Yapılan Değişiklikler
- ...

## İlgili Issue
Refs #<NO>

## Nasıl Test Edildi?
- `.\mvnw.cmd clean verify`

## Concurrency / Mimari Etkisi
- Paylaşılan veri:
- Koruma yöntemi:
- Alternatif ve neden seçilmedi:

## Kontrol Listesi
- [ ] Testler başarılı
- [ ] Mimari sınırlar korundu
- [ ] README/Swagger etkisi işlendi
- [ ] Mutable state dışarı açılmadı
```

## 8. Review Rotasyonu

- İbrahim, Fırat'ın en az bir PR'ını inceler.
- Fırat, Ahmet'in en az bir PR'ını inceler.
- Ahmet, İbrahim'in en az bir PR'ını inceler.
- Cem Bora ve Tolga kabul kriteri review'larını yapar.
- Sadece `LGTM` yerine teknik yorum bırakılır.

## 9. Ar-Ge Issue Kapatma Akışı

1. İlgili PR linkini kontrol et.
2. CI sonucunu kontrol et.
3. Kabul kriterlerini kontrol et.
4. Test veya evidence dosyasını kontrol et.
5. Main branch üzerinde smoke test yap.
6. Gerekirse changes requested ver.
7. Onay yorumunu yaz.
8. Issue'yu manuel kapat.

Standart yorum:

```markdown
## Ar-Ge Doğrulaması

- [ ] PR issue içinde paylaşılmış.
- [ ] CI başarılı.
- [ ] Review yorumları çözülmüş.
- [ ] Kabul kriterleri karşılanmış.
- [ ] Test/kanıt mevcut.
- [ ] Mimari sınırlar korunmuş.
- [ ] Main branch üzerinde doğrulandı.

**Karar:** APPROVED / CHANGES REQUESTED
**Doğrulayan:** @kullanici
**Not:**
```

## 10. Definition of Done

- Kod derlenir.
- İlgili testler geçer.
- `mvn clean verify` başarılıdır.
- Branch'ten PR açılmıştır.
- PR issue'ya bağlıdır.
- Anlamlı review tamamlanmıştır.
- CI başarılıdır.
- Gerekli doküman güncellenmiştir.
- Ar-Ge doğrulaması sonrası issue kapatılmıştır.
