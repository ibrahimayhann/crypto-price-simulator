package com.infina.price_simulator.engine;

import com.infina.price_simulator.model.PriceUpdateTask;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * Görev üreticisi ile fiyat güncelleme iş parçacıkları arasında kullanılan,
 * iş parçacığı güvenli görev kanalıdır.
 *
 * <p>Kuyruk bilinçli olarak sınırlı kapasitede tutulmuştur. Tüketiciler görevleri
 * yeterince hızlı işleyemediğinde, {@link #put(PriceUpdateTask)} metodu kuyruğun
 * kontrolsüz şekilde büyümesine izin vermek yerine üreticiyi bekletir.</p>
 */

public final class TaskQueue {

    /*
     * Gerçek görevler pozitif bir sıra numarasına ve desteklenen bir coin kimliğine
     * sahiptir. Bu nedenle değiştirilemez nitelikteki bu özel işaretçi, oluşturulan
     * herhangi bir fiyat güncelleme görevine karışmaz.
     */

    private static final PriceUpdateTask POISON_PILL =
            new PriceUpdateTask(-1L, "STOP", 0L);

    private final BlockingQueue<PriceUpdateTask> queue;
    private final int capacity;

/**
 * Sınırlı kapasiteye sahip bir görev kuyruğu oluşturur.
 *
 * @param capacity kuyrukta bekleyebilecek en fazla görev sayısı
 * @throws IllegalArgumentException kapasite değeri pozitif olmadığında
 */

    public TaskQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "Queue capacity must be greater than zero."
            );
        }

        this.capacity = capacity;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }


/**
 * Kuyruk dolu olduğunda bekleyerek görevi kuyruğa ekler.
 *
 * @param task kuyruğa eklenecek görev
 * @throws InterruptedException bekleyen iş parçacığının çalışması kesintiye uğratıldığında
 * @throws NullPointerException görev null olduğunda
 */
    public void put(PriceUpdateTask task)
            throws InterruptedException {

        queue.put(
                Objects.requireNonNull(
                        task,
                        "Task must not be null."
                )
        );
    }

/**
 * Kuyruk boş olduğunda bekleyerek sıradaki görevi kuyruktan çıkarır.
 *
 * @return kullanılabilir durumdaki sıradaki görev
 * @throws InterruptedException bekleyen iş parçacığının çalışması kesintiye uğratıldığında
 */
    public PriceUpdateTask take()
            throws InterruptedException {

        return queue.take();
    }

/**
 * Her iş parçacığı için bir adet sonlandırma işareti (poison pill) ekler.
 *
 * <p>Sınırlı kapasiteli bir kuyrukta bu metot kullanılmadan önce iş parçacıklarının
 * çalışıyor olması gerekir. Aksi hâlde, kuyruğun kalan kapasitesinden daha fazla
 * sonlandırma işareti eklenmesi, çağıran iş parçacığının süresiz olarak beklemesine
 * neden olabilir.</p>
 *
 * @param workerCount durdurulması gereken aktif iş parçacığı sayısı
 * @throws InterruptedException bekleyen iş parçacığı kesintiye uğratıldığında
 */

    public void putPoisonPills(int workerCount)
            throws InterruptedException {

        if (workerCount < 1) {
            throw new IllegalArgumentException(
                    "Worker count must be greater than zero."
            );
        }

        for (int index = 0; index < workerCount; index++) {
            put(POISON_PILL);
        }
    }

/**
 * İş parçacıkları tarafından kullanılan dahili durdurma sinyalini tanımlar.
 */

    public static boolean isPoisonPill(PriceUpdateTask task) {
        return POISON_PILL.equals(task);
    }

    public int size() {
        return queue.size();
    }

    public int capacity() {
        return capacity;
    }

    public int remainingCapacity() {
        return queue.remainingCapacity();
    }
}