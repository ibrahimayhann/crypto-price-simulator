package com.infina.price_simulator.util;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Deterministik ve anlamlı adlara sahip iş parçacıkları oluşturur.
 *
 * <p>Anlamlı adlar, günlük kayıtlarının ve JVM iş parçacığı dökümlerinin
 * daha kolay incelenmesini sağlar.</p>
 */


public final class WorkerThreadFactory implements ThreadFactory {

    private final String threadNamePrefix;
    private final AtomicInteger threadSequence = new AtomicInteger(1);

    public WorkerThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = Objects.requireNonNull(
                threadNamePrefix,
                "Thread name prefix must not be null."
        );

        if (threadNamePrefix.isBlank()) {
            throw new IllegalArgumentException(
                    "Thread name prefix must not be blank."
            );
        }
    }

    @Override
    public Thread newThread(Runnable task) {
        Objects.requireNonNull(task, "Task must not be null.");

        Thread thread = new Thread(
                task,
                threadNamePrefix + threadSequence.getAndIncrement()
        );


        /*
         * İşçi iş parçacıkları bilinçli olarak daemon olmayan şekilde oluşturulur.
         * Uygulamanın, executor'ı kontrollü biçimde kapatarak bu iş parçacıklarını
         * açıkça sonlandırması gerekir.
         */

        thread.setDaemon(false);

        return thread;
    }
}