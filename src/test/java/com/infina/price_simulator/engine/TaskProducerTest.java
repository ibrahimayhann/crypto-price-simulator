package com.infina.price_simulator.engine;

import com.infina.price_simulator.model.PriceUpdateTask;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskProducerTest {

    private static final Set<String> SUPPORTED_COIN_IDS =
            Set.of("BTC", "ETH", "SOL");

    private final TaskProducer taskProducer = new TaskProducer();

    @Test
    void sameSeedAndUpdateCountProduceSameTaskList() {
        List<PriceUpdateTask> firstRun =
                taskProducer.generate(1_000, 42L);

        List<PriceUpdateTask> secondRun =
                taskProducer.generate(1_000, 42L);

        assertEquals(firstRun, secondRun);
    }

    @Test
    void differentSeedsProduceDifferentTaskLists() {
        List<PriceUpdateTask> firstRun =
                taskProducer.generate(1_000, 42L);

        List<PriceUpdateTask> secondRun =
                taskProducer.generate(1_000, 43L);

        assertNotEquals(firstRun, secondRun);
    }

    @Test
    void generateReturnsRequestedNumberOfTasks() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(500, 42L);

        assertEquals(500, tasks.size());
    }

    @Test
    void generatedSequencesAreOneBasedAndContinuous() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(100, 42L);

        for (int index = 0; index < tasks.size(); index++) {
            long expectedSequence = index + 1L;

            assertEquals(
                    expectedSequence,
                    tasks.get(index).sequence()
            );
        }
    }

    @Test
    void generatedTasksContainOnlySupportedCoins() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(1_000, 42L);

        boolean everyCoinIsSupported = tasks.stream()
                .map(PriceUpdateTask::coinId)
                .allMatch(SUPPORTED_COIN_IDS::contains);

        assertTrue(everyCoinIsSupported);
    }

    @Test
    void generatedDeltasAreInsideConfiguredRangeAndNeverZero() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(1_000, 42L);

        for (PriceUpdateTask task : tasks) {
            assertTrue(task.delta() >= TaskProducer.MIN_DELTA);
            assertTrue(task.delta() <= TaskProducer.MAX_DELTA);
            assertNotEquals(0L, task.delta());
        }
    }

    @Test
    void generatedTaskListIsImmutable() {
        List<PriceUpdateTask> tasks =
                taskProducer.generate(10, 42L);

        assertThrows(
                UnsupportedOperationException.class,
                () -> tasks.add(
                        new PriceUpdateTask(11L, "BTC", 10L)
                )
        );
    }

    @Test
    void generateRejectsZeroUpdateCount() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> taskProducer.generate(0, 42L)
        );

        assertFalse(exception.getMessage().isBlank());
    }

    @Test
    void generateRejectsNegativeUpdateCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> taskProducer.generate(-1, 42L)
        );
    }

    @Test
    void generateRejectsUpdateCountAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> taskProducer.generate(100_001, 42L)
        );
    }
}