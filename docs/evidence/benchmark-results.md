# Benchmark Sonuçları

- Tarih: `2026-07-14`
- İşletim sistemi: `Windows 11 (NT 10.0.26200.0)`
- Java: `25+37-LTS-3491 (Java HotSpot 64-Bit Server VM)`
- CPU: `13th Gen Intel(R) Core(TM) i7-13650HX`
- Seed: `42`
- Updates: `50 000`
- Task DEBUG logları: `KAPALI`

| Workers | Unsafe ms | Safe ms | Unsafe task/s | Safe task/s | Safe invariant |
|---:|---:|---:|---:|---:|---|
| 1 | `37` | `21` | `1 351 351` | `2 380 952` | `Başarılı` |
| 2 | `17` | `15` | `2 941 176` | `3 225 806` | `Başarılı` |
| 4 | `9` | `8` | `5 555 556` | `6 549 252` | `Başarılı` |
| 8 | `11` | `12` | `4 545 455` | `4 094 481` | `Başarılı` |

## Yorum

```text
Worker=1: Hem UNSAFE hem SAFE tek thread üzerinde çalıştığı için race condition
oluşmaz; Safe'in Unsafe'ten hızlı çıkması JIT optimizasyonunun AtomicLong'u
sıradan long kadar hızlı derlediğini gösterir.

Worker=2→4: Paralelizm arttıkça throughput ölçeklenebilir biçimde yükseliyor.
i7-13650HX'in P-core / E-core mimarisinde 4 worker optimal nokta; işçi sayısı
CPU çekirdek sınırının altında kaldığından context switching maliyeti minimumdur.

Worker=8: Throughput düşüyor. Coin başına yalnızca 3 farklı coin varken 8 worker
lock contention'ı (SafeCoinState ReentrantLock) artırır; thread'ler kilidi bekleyerek
park durumuna girer ve context switching maliyeti kazanımı gölgeler.

Sonuç: Beklenen ölçek eğrisi — düşük thread sayısında doğrusal büyüme, yüksek
thread sayısında lock contention kaynaklı performans düşüşü — gerçek çalıştırmada
gözlemlenmiştir.
```
