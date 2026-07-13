package com.infina.price_simulator.engine;

import com.infina.price_simulator.model.PriceUpdateTask;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simülasyon için deterministik fiyat güncelleme iş yükleri üretir.

 * Oluşturulan görev listesi; beklenen sonuç, güvenli olmayan ve güvenli
 * simülasyon çalıştırmalarında yeniden kullanılır. Bu nedenle döndürülen liste
 * değiştirilemezdir ve oluşturulduktan sonra üzerinde değişiklik yapılmamalıdır.
 */
@Component
public class TaskProducer {

    static final int MIN_UPDATE_COUNT = 1;
    static final int MAX_UPDATE_COUNT = 100_000;

    /*
     * Ödev, kullanılması zorunlu belirli bir delta aralığı tanımlamamaktadır.
     * Seçilen aralık; üretim sürecinin öngörülebilir, test edilebilir ve kolayca
     * dokümante edilebilir olması için tek bir yerde tutulmaktadır.
     */
    static final int MIN_DELTA = -100;
    static final int MAX_DELTA = 100;

    private static final List<String> SUPPORTED_COIN_IDS =
            List.of("BTC", "ETH", "SOL");

    /**
     * Deterministik ve değiştirilemez bir görev listesi oluşturur.

     * @param updateCount oluşturulacak fiyat güncelleme görevi sayısı
     * @param seed        aynı iş yükünü yeniden oluşturmak için kullanılan başlangıç değeri
     * @return tam olarak {@code updateCount} adet görev içeren değiştirilemez liste
     * @throws IllegalArgumentException güncelleme sayısı desteklenen aralığın dışında olduğunda
     */
    public List<PriceUpdateTask> generate(int updateCount, long seed) {
        validateUpdateCount(updateCount);

        Random random = new Random(seed);
        List<PriceUpdateTask> tasks = new ArrayList<>(updateCount);

        for (long sequence = 1; sequence <= updateCount; sequence++) {
            String coinId = selectCoin(random);
            long delta = generateNonZeroDelta(random);

            tasks.add(new PriceUpdateTask(sequence, coinId, delta));
        }

        /*
         * Beklenen, güvenli olmayan ve güvenli çalıştırmaların tamamen aynı girdiyi
         * işlemesi gerekir. Değiştirilemez bir liste, bir aşamanın başka bir aşamanın
         * kullandığı iş yükünü yanlışlıkla değiştirmesini önler.
         */

        return List.copyOf(tasks);
    }

    private String selectCoin(Random random) {
        int coinIndex = random.nextInt(SUPPORTED_COIN_IDS.size());
        return SUPPORTED_COIN_IDS.get(coinIndex);
    }

    private long generateNonZeroDelta(Random random) {
        int delta;

        do {
            delta = random.nextInt(MAX_DELTA - MIN_DELTA + 1) + MIN_DELTA;
        } while (delta == 0);

        return delta;
    }

    private void validateUpdateCount(int updateCount) {
        if (updateCount < MIN_UPDATE_COUNT
                || updateCount > MAX_UPDATE_COUNT) {

            throw new IllegalArgumentException(
                    "Update count must be between "
                            + MIN_UPDATE_COUNT
                            + " and "
                            + MAX_UPDATE_COUNT
                            + "."
            );
        }
    }
}