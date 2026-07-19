# Eşzamanlı Kripto Fiyat Simülatörü

## Proje Hakkında

Bellek içinde çalışan (in-memory) bu simülatör, bir görev üretici tarafından oluşturulan N adet kripto fiyat güncelleme görevini (PriceUpdateTask) `ArrayBlockingQueue` tabanlı bir kuyruğa atar ve sabit boyutlu bir Thread Pool (işçi havuzu) ile eşzamanlı olarak işler. Aynı veri setini beklenen (expected), güvensiz (unsafe) ve güvenli (safe) olmak üzere üç farklı senaryoda koşturur. Ortak sayaç (processedCount) ve her bir coinin state'i üzerindeki olası "race condition" ve "lost update" problemleri uygulamalı olarak gösterilmiş, ardından `AtomicLong` ve `ReentrantLock` yardımıyla bu sorunlar güvenle çözülmüştür.

## 1. Grup Bilgileri

| Alan | Bilgi |
|---|---|
| Grup adı | <Binary Minds> |
| Grup üyeleri (5) | <Cem BORA, İbrahim AYHAN, Tolga, Firat ASI, Ahmet KÖRELİ> |
| GitHub repo linki | <https://github.com/ibrahimayhann/crypto-price-simulator> |
| Pull Request linkleri | <https://github.com/ibrahimayhann/crypto-price-simulator/pulls> |
| Conflict çözülen dosya | <README.md> |
| Conflict çözüm commit / PR | <commit: 75f3037 / readme-memory-a, readme-memory-b> |
| Yapılan bonus (Bonus-A) | <virtual threads> |


## Kullanılan Teknolojiler

Java 21, Spring Boot, Maven, Git/GitHub, Swagger/OpenAPI (springdoc-openapi), JUnit 5, Virtual Threads (Bonus A).

## Uygulamayı Çalıştırma

1. `git clone <https://github.com/ibrahimayhann/crypto-price-simulator.git>`
2. IntelliJ ile açın (Open → `pom.xml`).
3. `mvn clean verify` komutu ile testleri doğrulayın.
4. `mvn spring-boot:run` komutu ile uygulamayı başlatın.
5. `http://localhost:8080` üzerinde ayağa kalkar.

## Swagger Adresi

- http://localhost:8080/swagger-ui/index.html (API uç noktalarını arayüzden test edebilirsiniz.)
- http://localhost:8080/api-docs (OpenAPI JSON şeması)

## Endpoint'ler

| Endpoint | Ne yapar? |
|---|---|
| `POST /simulate?updates=50000&workers=4&seed=42` | Görevleri üretir, kuyruğa koyar, havuzla işler, biter. 409: aynı anda ikinci istek. 400: geçersiz parametre (örn updates>100k). |
| `GET /coins` | Son simülasyondaki güvenli coin durumlarını liste halinde döner. |
| `GET /stats` | Son simülasyon sonucu (beklenen/güvensiz/güvenli, süre, throughput, invariant başarı durumu). 404: henüz simülasyon yok. |

## Mimari Akış

```text
Task Producer → ArrayBlockingQueue<PriceUpdateTask> → Sabit Worker Pool (FixedThreadPool) → Coin State + Sayaçlar
```

- **TaskProducer**: Verilen seed ile `updates` sayısı kadar rastgele BTC, ETH, SOL güncellemesi (PriceUpdateTask) üretir.
- **SimulationEngine**: Görevleri alır, kuyruğa basar. Worker thread'ler bu kuyruktan görev çekip `CoinState` üzerinde uygular.
- **PriceWorker / TaskProcessor**: Kuyruktan çekilen görevlerin hangi coine ait olduğunu bulup o coine `delta` uygular ve processed sayaçlarını günceller.
- **SimulationController / Service**: HTTP isteklerini karşılar, Engine'i çağırır ve sonuçları formatlayıp REST response olarak sunar.

## Tasarım Kararları

| Karar noktası | Kararımız | Neden? (+alternatif karşılaştırması) |
|---|---|---|
| Görev kuyruğu | `ArrayBlockingQueue(1000)` | Sınırlı (bounded) yapısı sayesinde OOM (Out Of Memory) hatalarını önler, dolduğunda Producer'ı durdurarak backpressure sağlar. Sınırsız `LinkedBlockingQueue`'a göre çok daha güvenlidir. |
| Worker havuzu | `FixedThreadPool(workers)` | Her görev için yeni thread açma (ve yok etme) maliyetinden kaçınmak için. Sabit boyut, CPU çekirdek sayısına göre optimize edilebilir. |
| Güvenli sayaç | `AtomicLong` | Sadece tek bir sayacın (`value++`) güvenli artırılması gerektiği için en yüksek performansı CAS (Compare-And-Swap) ile non-blocking sağlar. `synchronized` kullanmaktan daha hafiftir. |
| Coin kilidi | `ReentrantLock` | Bir coinin fiyatı, güncelleme sayısı ve son deltasının aynı anda tutarlı olması gerekir. (Compound action). Bu yüzden lock mekanizması seçildi. |
| Lock kapsamı | Coin başına (Per-Coin Lock) | Sadece güncellenen coin kilitlenir (ör. BTC güncellenirken ETH güncellenebilir). Global tek lock olsaydı uygulamanın paralelliği yok olurdu. |
| İşlerin tamamlanması | `CountDownLatch` | Main thread'in tüm asenkron görevler bitene kadar beklemesi için en uygun araçtır. Görev sayısıyla başlatılır, her biten görevde azaltılır. |
| Graceful shutdown | `shutdown + awaitTermination` | Havuzun işi bitirmesi için makul bir süre (30sn) tanır, süre aşılırsa veya interrupt edilirse `shutdownNow()` ile zorla kapatır, leak bırakmaz. |
| Sonucun paylaşılması | `SimulationRunResult` (Immutable Snapshot) | Simülasyon tamamen bittiğinde o anki thread-safe snapshot alınır ve değiştirilemez objelerle Controller'a dönülür, böylece concurrency sorunları arayüze sızmaz. |
| İkinci simülasyon isteği | `AtomicBoolean` | Simülasyon çalışıyorken HTTP'den yeni istek gelirse 409 dönmesi için `compareAndSet` ile lock görevi görür. `finally` bloğunda `false` yapılarak bırakılır. |

## Race Condition Gözlemi

1. **UnsafeCounter'da Oku-Artır-Yaz Sorunu:** `value++` işlemi bayt kodunda 3 adımdır (Oku, Artır, Yaz). İki thread aynı anda eski değeri okur ve aynı yeni değeri yazarsa bir artış kaybolur (Lost Update).
2. **UnsafeCoinState'te Compound Sorun:** Coinin fiyatı ile toplam güncelleme sayısı aynı anda (atomik) yazılmadığı için bir thread fiyatı güncelleyip tam sayaç artıracakken CPU'dan alınırsa, diğer thread o anki yarı-güncel state'i okuyup bozar.

```text
BTC   beklenen: 59.454 | güvenli: 59.454 ✓ | güvensiz: 59.521 ✗
Sayaç beklenen: 50.000 | güvenli: 50.000 ✓ | güvensiz: 49.390 ✗
```

> Gözlem: Yaptığımız testlerde (50.000 update, Worker=4), **5 çalıştırmanın 5'inde de** Unsafe modunda veri kayıpları/sapmalar (10 ile 800 arası kayıp) yaşandı. Hata deterministik değildi, her seferinde farklı sayılarda lost update oluştu. (OS zamanlamasına bağlı).

## Güvenli Çözüm

Çok alanlı (currentPrice, updateCount vb.) durumu korumak için `ReentrantLock` kullandık. Böylece bir thread ilgili coin üzerinde işlem yaparken diğerleri onu bekler (Mutual Exclusion).

```java
@Override
public void applyDelta(long delta) {
    lock.lock();
    try {
        currentPrice += delta;
        updateCount++;
        lastDelta = delta;
        lastUpdatedBy = Thread.currentThread().getName();
    } finally {
        lock.unlock(); // Hata fırlatılsa bile kilit kesinlikle bırakılır.
    }
}
```

## Invariant ve Doğruluk Kanıtı

```text
safePrice       == initialPrice + sum(all deltas)    ->  PASSED ✓
safeUpdateCount == o coin için üretilen görev sayısı ->  PASSED ✓
safeProcessedCount == submittedUpdates               ->  PASSED ✓
```

## Performans Sonuçları

- Tarih: `2026-07-14`
- CPU: `13th Gen Intel(R) Core(TM) i7-13650HX`
- Task DEBUG logları: `KAPALI`
- Updates: 50.000

| Updates | Workers | Süre (Platform) | Throughput (Task/s) | Invariant |
|---:|---:|---:|---:|---|
| 50.000 | 1 | 21 ms | 2.380.952 | Başarılı |
| 50.000 | 2 | 15 ms | 3.225.806 | Başarılı |
| 50.000 | 4 | 8 ms | 6.549.252 | Başarılı |
| 50.000 | 8 | 12 ms | 4.094.481 | Başarılı |

> Yorum: Worker sayısı 1'den 4'e çıkarken sürede ve throughput'ta belirgin bir iyileşme gördük. Ancak Worker=8 yapıldığında süre 12ms'ye uzadı. Bunun sebebi sadece 3 adet coin (BTC, ETH, SOL) kilit noktası olmasıdır. 8 thread aynı anda 3 kilide (ReentrantLock) ulaşmaya çalıştığında Lock Contention (kilit çekişmesi) artar ve Thread Context Switch (bağlam değiştirme) maliyeti kazanımları gölgeler.

## ReentrantLock ve synchronized Karşılaştırması

Projede coin state koruması için `synchronized` yerine **`ReentrantLock`** kullandık. Sebebi: 
1. **Adalet (Fairness):** Gerekirse kuyrukta bekleyen en eski thread'e öncelik verebilecek `new ReentrantLock(true)` yapısını desteklemesi.
2. **Esneklik:** `tryLock` ile zaman aşımı koyabilme veya kilidi bir metodda alıp başka metodda bırakabilme yeteneği sağlaması.
3. Kilit mekanizmasını **Coin Başına** yaptık. Global bir kilit atsaydık, BTC güncellenirken ETH boş yere kilitli kalırdı ve sistem Single-Thread hantallığında çalışırdı.

## Thread Dump İncelemesi

> "Capture Thread Dump" ile alınan kesit:

```text
"safe-worker-1" prio=5 Id=27 WAITING on java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionObject@6d8a00e3
	at java.base/jdk.internal.misc.Unsafe.park(Native Method)
	at java.base/java.util.concurrent.locks.LockSupport.park(LockSupport.java:371)
	at java.base/java.util.concurrent.locks.AbstractQueuedSynchronizer$ConditionNode.block(AbstractQueuedSynchronizer.java:519)
	at java.base/java.util.concurrent.ArrayBlockingQueue.take(ArrayBlockingQueue.java:425)
	at app//com.infina.price_simulator.engine.PriceWorker.run(PriceWorker.java:72)
```

- **Kaç worker var?** Havuz ayarımız 4 olduğu için tam 4 adet `safe-worker-x` thread'i mevcut. Sistemin kontrolü dışında binlerce thread YOK.
- **Hangi state'teler?** Kuyrukta görev kalmadığında (veya yetişemediklerinde) işçi thread'ler `ArrayBlockingQueue.take()` metodunda **WAITING (parking)** durumundadırlar. Busy-wait yapıp CPU'yu yormazlar.
- **Lock çekişmesi/deadlock var mı?** Yapılan incelemede deadlock saptanmamış, "Found one Java-level deadlock" uyarısı alınmamıştır.

## Merge Conflict Deneyimi

- **Branch isimleri:** `conflict/readme-memory-a` ve `conflict/readme-memory-b`
- **Conflict çıkan dosya / bölüm:** `README.md` dosyasındaki proje açıklaması / bellek kullanımı bölümü.
- **İki branch'in farklı değişikliği:** Aynı README satırı iki branch'te farklı ifadelerle güncellendi. Böylece bilinçli olarak gerçek bir merge conflict oluşturuldu.
- **Hangi içerik korundu:** Her iki branch'teki anlamlı açıklamalar birleştirilerek tek, tutarlı ve tekrarsız bir metin oluşturuldu.
- **Çözüm aracı:** Github arayuzu kullanildi.
- **Çözüm commit / PR linki:** `<commit: 75f3037 / PR: https://github.com/ibrahimayhann/crypto-price-simulator/pull/3>`
- **Ne öğrendik:** Aynı dosyanın aynı satırlarında paralel değişiklik yapıldığında Git'in otomatik birleştirme yapamadığını gördük. Conflict işaretlerini inceleyerek hangi içeriğin korunacağına ekipçe karar verdik. Küçük, odaklı commit ve PR'ların conflict çözümünü kolaylaştırdığını öğrendik.
## Testler

Proje geniş bir JUnit ve entegrasyon test ağıyla örülmüştür. Testleri koşmak için: `mvn clean verify`

- Seed tekrarlanabilirlik testi (TaskProducerTest)
- Beklenen fiyat hesabı testi (ExpectedCalculatorTest)
- Güvenli sayaç testi (SafeCounterTest)
- Coin invariant testi (InvariantCheckerTest)
- Parametre validation (HTTP 400) testi (GlobalExceptionHandlerTest)
- Controller integration testi (SimulationControllerIntegrationTest)
- Graceful Shutdown testi (SimulationEngineTest)

## 8. Zorunlu Özellikler — Öz Değerlendirme

- [X] /simulate, /coins, /stats çalışıyor
- [X] Geçersiz parametre → HTTP 400, ikinci eşzamanlı istek → HTTP 409
- [X] Aynı görev listesi (immutable, tek üretim) safe ve unsafe'de kullanılıyor
- [X] BlockingQueue + sabit thread pool (her görev için yeni thread yok)
- [X] Güvensiz sürüm hatayı gösteriyor; güvenli sürüm invariant'ı sağlıyor
- [X] En az bir yerde ReentrantLock kullanıldı
- [X] Graceful shutdown; işlerin bitmesi bekleniyor
- [X] Seed ile tekrarlanabilir görev üretimi
- [X] throughput/süre + 1/2/4/8 worker tablosu
- [X] Thread dump alındı ve README'de yorumlandı
- [X] Swagger çalışıyor, adres README'de
- [X] Unit + en az 1 integration test
- [X] En az 3 branch, 2 PR, 2 review, 1 çözülmüş conflict


## Grup Üyeleri ve Katkıları

| Üye | Sorumluluk | Branch | Pull Request | Review |
|---|---|---|---|---|
| İbrahim | Model, State & Core Testler | `feature/core` | 
| Fırat | Producer, Queue, Engine & Benchmark | `feature/engine` | 
| Ahmet | Controller, Service, DTO, Exception & OpenAPI | `feature/api` | 
| Cem Bora | Ar-Ge & Invariant Kontrolleri | `feature/docs` | 
| Tolga | Mimari Gözetim, Teslim Raporu | `feature/final` | 

## Bonus Çalışmalar

**Bonus A (Java 21 Virtual Threads) Başarıyla Tamamlandı!**
Mevcut fixed thread pool sistemine ek olarak, `Executors.newVirtualThreadPerTaskExecutor` ile görev başına sanal thread yaratan bir mekanizma projeye eklendi. 

- **CPU-Bound Senaryosu:** 50.000 işlem yapıldığında Platform thread'ler **6ms**, Virtual Thread'ler ise **89ms** sürdü. (Hesaplama işlemlerinde virtual thread'ler sürekli OS thread'lerine context switch yarattığı için **yavaş kaldı**).
- **I/O-Bound Senaryosu:** İşlem içine 1ms `Thread.sleep()` konulduğunda Platform (4 worker) **479ms**, Virtual Thread'ler ise **8ms** sürdü! (Virtual thread'ler uyurken işletim sistemi thread'ini bloke etmediği için inanılmaz bir hız kazandı). Bu muazzam fark kanıtlanmış oldu.
