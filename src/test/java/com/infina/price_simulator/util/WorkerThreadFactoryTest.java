package com.infina.price_simulator.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkerThreadFactoryTest {

    @Test
    void createsThreadsWithSequentialNames() {
        WorkerThreadFactory factory =
                new WorkerThreadFactory("safe-worker-");

        Thread first = factory.newThread(() -> {
        });

        Thread second = factory.newThread(() -> {
        });

        assertEquals("safe-worker-1", first.getName());
        assertEquals("safe-worker-2", second.getName());
    }

    @Test
    void createsNonDaemonThreads() {
        WorkerThreadFactory factory =
                new WorkerThreadFactory("unsafe-worker-");

        Thread thread = factory.newThread(() -> {
        });

        assertFalse(thread.isDaemon());
    }

    @Test
    void rejectsBlankPrefix() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WorkerThreadFactory(" ")
        );
    }

    @Test
    void rejectsNullTask() {
        WorkerThreadFactory factory =
                new WorkerThreadFactory("safe-worker-");

        assertThrows(
                NullPointerException.class,
                () -> factory.newThread(null)
        );
    }
}