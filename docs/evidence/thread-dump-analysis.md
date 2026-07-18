# Thread Dump Analizi

- Tarih: `2026-07-18`
- Dump komutu: `jcmd 42140 Thread.print > docs\evidence\thread-dump.txt`
- Worker parametresi: `4 (safe-worker)`
- Ham dosya: `thread-dump.txt`

| Kontrol | Gözlem |
|---|---|
| Safe worker adları | `safe-worker-1`, `safe-worker-2`, `safe-worker-3`, `safe-worker-4` |
| Unsafe worker adları | `Yok — ThreadDumpEvidenceTest yalnızca safe modda çalışır` |
| WAITING worker'lar | `4/4 — tümü WAITING (parking) durumunda` |
| BlockingQueue.take gözlemi | `java.util.concurrent.ArrayBlockingQueue.take → tüm safe-worker'larda görüldü` |
| RUNNABLE worker'lar | `0 — kuyrukta görev olmadığından hiçbiri aktif değil` |
| ReentrantLock contention | `Yok — worker'lar kuyrukta bloklu, lock yarışması için aktif değil` |
| Java-level deadlock | `Yok` |
| Kontrolsüz thread oluşturma | `Yok` |

## Sonuç

```text
jcmd Thread.print çıktısı WorkerThreadFactory tarafından oluşturulan dört thread'i
açıkça "safe-worker-{N}" ismiyle gösterdi. Tüm worker thread'leri WAITING (parking)
durumunda ve ArrayBlockingQueue.take() çağrısı içindeydi.

Bu beklenen davranıştır: kuyrukta görev bulunmadığında worker, poison pill alana
kadar bloklama noktasında park eder. Böylece busy-wait yapmadan CPU'yu serbest
bırakır ve işletim sistemine geri verir.

Thread dökümünde Java-level deadlock bölümü yer almıyor; bu, "Found one Java-level
deadlock:" satırının jcmd tarafından yazılmadığını ve dolayısıyla deadlock olmadığını
kanıtlar.

Kontrolsüz thread oluşturma da gözlemlenmedi: tüm uygulama thread'leri
WorkerThreadFactory üzerinden, yalnızca Executors.newFixedThreadPool tarafından
oluşturuldu. Ad şeması deterministik ve izlenebilir.

Sonuç: Thread adlandırma, blocking mekanizması ve thread havuzu yönetimi tasarıma
uygun biçimde çalışmaktadır.
```
