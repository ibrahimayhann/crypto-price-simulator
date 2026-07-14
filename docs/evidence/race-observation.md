# Race Condition Observation

- Tarih: `2026-07-14`
- Seed: `42`
- Updates: `50 000`
- Workers: `4`
- Run sayısı: `5`

| Run | Unsafe processed | Safe processed | Unsafe counter sapma | Safe invariant |
|---:|---:|---:|---|---|
| 1 | `49 390` | `50 000` | `KAYIP = 610` | `Başarılı` |
| 2 | `49 103` | `50 000` | `KAYIP = 897` | `Başarılı` |
| 3 | `49 991` | `50 000` | `KAYIP = 9` | `Başarılı` |
| 4 | `49 986` | `50 000` | `KAYIP = 14` | `Başarılı` |
| 5 | `49 972` | `50 000` | `KAYIP = 28` | `Başarılı` |

## Özet

- Unsafe counter sapma görülen run: `5/5`
- Unsafe coin sapma görülen run: `5/5`
- Safe invariant başarılı run: `5/5`
- Unsafe sonuç yapay olarak bozuldu: `Hayır`

## Yorum

```text
Worker=1 koşulunda race condition gözlemlenmez; tek thread sıralı çalıştığından
read-modify-write döngüsünde araya giren başka thread yoktur.

Worker=4 koşulunda her run'da farklı miktarda sayaç kaybı oluştu (9–897 arası).
Bu tutarsızlık, race condition'ın deterministik değil zamanlama (scheduling)
bağımlı olduğunu kanıtlar. UnsafeCounter'ın "long value++" operasyonu bytecode
seviyesinde GETFIELD / LADD / PUTFIELD olarak üçe ayrıldığından, iki thread aynı
anda GETFIELD yaptığında ikisi de aynı değeri okur (stale read), her biri +1 ekler
ve aynı değeri yazar: bir artış kaybolur (lost update).

UnsafeCoinState'te de aynı senaryo geçerlidir; currentPrice ve updateCount alanları
synchronized olmadan güncellenir, dolayısıyla coin durumu invaryantsız sonuçlanır.

Safe modu ReentrantLock ile korunan SafeCoinState ve AtomicLong tabanlı SafeCounter
sayesinde 5/5 run'da 50 000/50 000 eksiksiz sayım ve doğru coin durumu sağladı.
Sonuç; eşzamanlılık koruma mekanizmaları olmaksızın veri bütünlüğünün sağlanamadığını
kanıtlar.
```
