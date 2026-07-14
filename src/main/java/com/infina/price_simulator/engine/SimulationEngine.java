package com.infina.price_simulator.engine;

import com.infina.price_simulator.counter.Counter;
import com.infina.price_simulator.counter.SafeCounter;
import com.infina.price_simulator.counter.UnsafeCounter;
import com.infina.price_simulator.model.CoinRunSnapshot;
import com.infina.price_simulator.model.PriceUpdateTask;
import com.infina.price_simulator.model.SimulationRunResult;
import com.infina.price_simulator.state.CoinState;
import com.infina.price_simulator.state.SafeCoinState;
import com.infina.price_simulator.state.UnsafeCoinState;
import com.infina.price_simulator.util.WorkerThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Birbirinden bağımsız güvenli veya güvenli olmayan bir simülasyon iş yükünü çalıştırır.
 *
 * <p>Her çağrıda yeni bir kuyruk, sayaç, coin durum koleksiyonu ve sabit boyutlu
 * iş parçacığı havuzu oluşturulur. Değiştirilebilir çalışma durumu, farklı
 * çağrılar arasında paylaşılmaz.</p>
 */

@Component
public class SimulationEngine {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(SimulationEngine.class);

    static final int MIN_WORKER_COUNT = 1;
    static final int MAX_WORKER_COUNT = 16;

    private static final int MAX_QUEUE_CAPACITY = 1_000;
    private static final long TASK_COMPLETION_TIMEOUT_SECONDS = 60L;
    private static final long SHUTDOWN_TIMEOUT_SECONDS = 30L;

    public SimulationRunResult run(
            List<PriceUpdateTask> sourceTasks,
            int workerCount,
            SimulationMode mode
    ) {
        List<PriceUpdateTask> tasks =
                validateAndCopyTasks(sourceTasks);

        validateWorkerCount(workerCount);
        Objects.requireNonNull(mode, "Simulation mode must not be null.");

        Map<String, CoinState> coinStates =
                createCoinStates(mode);

        Counter processedCounter =
                createCounter(mode);

        int queueCapacity =
                Math.min(tasks.size(), MAX_QUEUE_CAPACITY);

        TaskQueue taskQueue =
                new TaskQueue(queueCapacity);

        CountDownLatch completionLatch =
                new CountDownLatch(tasks.size());

        AtomicReference<Throwable> firstFailure =
                new AtomicReference<>();

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        workerCount,
                        new WorkerThreadFactory(
                                mode.getThreadNamePrefix()
                        )
                );

        long startedAt = System.nanoTime();

        try {
            startWorkers(
                    executor,
                    workerCount,
                    taskQueue,
                    coinStates,
                    processedCounter,
                    completionLatch,
                    firstFailure
            );

            enqueueTasks(taskQueue, tasks);
            taskQueue.putPoisonPills(workerCount);

            boolean completed = completionLatch.await(
                    TASK_COMPLETION_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS
            );

            if (!completed) {
                throw new SimulationEngineException(
                        "Simulation tasks did not complete within "
                                + TASK_COMPLETION_TIMEOUT_SECONDS
                                + " seconds."
                );
            }

            shutdownGracefully(executor);

            Throwable workerFailure = firstFailure.get();

            if (workerFailure != null) {
                throw new SimulationEngineException(
                        "At least one worker failed during simulation.",
                        workerFailure
                );
            }

            long elapsedNanos =
                    System.nanoTime() - startedAt;

            SimulationRunResult result =
                    createResult(
                            mode,
                            tasks.size(),
                            workerCount,
                            processedCounter,
                            elapsedNanos,
                            coinStates
                    );

            LOGGER.info(
                    "Simulation completed: mode={}, updates={}, "
                            + "workers={}, elapsedMs={}, "
                            + "throughputPerSec={}",
                    mode,
                    tasks.size(),
                    workerCount,
                    result.elapsedMillis(),
                    Math.round(result.throughputPerSecond())
            );

            return result;
        } catch (InterruptedException exception) {
            shutdownImmediately(executor);
            Thread.currentThread().interrupt();

            throw new SimulationEngineException(
                    "Simulation was interrupted.",
                    exception
            );
        } catch (RuntimeException exception) {
            shutdownImmediately(executor);
            throw exception;
        }
    }

    private void startWorkers(
            ExecutorService executor,
            int workerCount,
            TaskQueue taskQueue,
            Map<String, CoinState> coinStates,
            Counter processedCounter,
            CountDownLatch completionLatch,
            AtomicReference<Throwable> firstFailure
    ) {
        for (int index = 0; index < workerCount; index++) {
            executor.submit(
                    new PriceWorker(
                            taskQueue,
                            coinStates,
                            processedCounter,
                            completionLatch,
                            firstFailure
                    )
            );
        }
    }

    private void enqueueTasks(
            TaskQueue taskQueue,
            List<PriceUpdateTask> tasks
    ) throws InterruptedException {

        for (PriceUpdateTask task : tasks) {
            taskQueue.put(task);
        }
    }

    private Map<String, CoinState> createCoinStates(
            SimulationMode mode
    ) {
        Map<String, CoinState> states =
                new LinkedHashMap<>();

        states.put(
                "BTC",
                createCoinState(mode, "BTC", 60_000L)
        );

        states.put(
                "ETH",
                createCoinState(mode, "ETH", 3_000L)
        );

        states.put(
                "SOL",
                createCoinState(mode, "SOL", 150L)
        );

        /*
         * The map structure is read-only after worker submission. Individual
         * CoinState instances provide safe or unsafe update behavior.
         */
        return Collections.unmodifiableMap(states);
    }

    private CoinState createCoinState(
            SimulationMode mode,
            String id,
            long initialPrice
    ) {
        return switch (mode) {
            case SAFE ->
                    new SafeCoinState(id, initialPrice);

            case UNSAFE ->
                    new UnsafeCoinState(id, initialPrice);
        };
    }

    private Counter createCounter(SimulationMode mode) {
        return switch (mode) {
            case SAFE -> new SafeCounter();
            case UNSAFE -> new UnsafeCounter();
        };
    }

    private SimulationRunResult createResult(
            SimulationMode mode,
            int submittedUpdates,
            int workerCount,
            Counter counter,
            long elapsedNanos,
            Map<String, CoinState> coinStates
    ) {
        List<CoinRunSnapshot> coinResults =
                new ArrayList<>(coinStates.size());

        /*
         * Executor'ın tamamen sonlanması, değiştirilemez nihai durum kopyaları
         * oluşturulurken hiçbir işçi iş parçacığının coin durumlarını
         * değiştiremeyeceğini garanti eder.
         */

        for (CoinState coin : coinStates.values()) {
            coinResults.add(
                    new CoinRunSnapshot(
                            coin.getId(),
                            coin.getInitialPrice(),
                            coin.getCurrentPrice(),
                            coin.getUpdateCount(),
                            coin.getLastDelta(),
                            coin.getLastUpdatedBy()
                    )
            );
        }

        return new SimulationRunResult(
                mode,
                submittedUpdates,
                counter.get(),
                workerCount,
                elapsedNanos,
                coinResults
        );
    }

    private void shutdownGracefully(
            ExecutorService executor
    ) throws InterruptedException {

        executor.shutdown();

        if (executor.awaitTermination(
                SHUTDOWN_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        )) {
            return;
        }

        LOGGER.warn(
                "Executor did not terminate in time; forcing shutdown."
        );

        executor.shutdownNow();

        if (!executor.awaitTermination(
                SHUTDOWN_TIMEOUT_SECONDS,
                TimeUnit.SECONDS
        )) {
            throw new SimulationEngineException(
                    "Executor did not terminate after forced shutdown."
            );
        }
    }

    private void shutdownImmediately(
            ExecutorService executor
    ) {
        if (!executor.isTerminated()) {
            executor.shutdownNow();
        }
    }

    private List<PriceUpdateTask> validateAndCopyTasks(
            List<PriceUpdateTask> sourceTasks
    ) {
        Objects.requireNonNull(
                sourceTasks,
                "Task list must not be null."
        );

        if (sourceTasks.isEmpty()) {
            throw new IllegalArgumentException(
                    "Task list must not be empty."
            );
        }

        if (sourceTasks.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "Task list must not contain null values."
            );
        }

        /*
         * Savunma amaçlı oluşturulan değiştirilemez kopya, işçi iş parçacıkları görevleri
         * işlerken çağıran kodun görev listesini değiştirmesini önler.
         */
        return List.copyOf(sourceTasks);
    }

    private void validateWorkerCount(int workerCount) {
        if (workerCount < MIN_WORKER_COUNT
                || workerCount > MAX_WORKER_COUNT) {

            throw new IllegalArgumentException(
                    "Worker count must be between "
                            + MIN_WORKER_COUNT
                            + " and "
                            + MAX_WORKER_COUNT
                            + "."
            );
        }
    }
}