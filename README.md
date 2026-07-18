# Concurrent Crypto Price Simulator

A Spring Boot project that processes deterministic cryptocurrency price-update tasks through expected, unsafe, and safe execution paths. The same immutable task list is used in all three paths so concurrency errors can be observed and the safe result can be verified with invariants.

The simulator starts with three in-memory coins:

| Coin | Initial price |
|---|---:|
| BTC | 60,000 |
| ETH | 3,000 |
| SOL | 150 |

## Getting Started

### Requirements

- Java 21
- Maven

### Clone and verify

```powershell
git clone https://github.com/ibrahimayhann/crypto-price-simulator.git
cd crypto-price-simulator
mvn.cmd clean verify
```

### Run the application

```powershell
mvn.cmd spring-boot:run
```

The application starts on `http://localhost:8080`.

## Technology Stack

> !!! TEAM INPUT REQUIRED: Complete the team-owned reasons for using each technology. Do not replace these markers with assumptions.

| Technology | Version or usage | Why the team used it |
|---|---|---|
| Java | 21 | !!! |
| Spring Boot | 4.1.0 | !!! |
| Maven | Build and dependency management | !!! |
| Spring Web MVC | REST API | !!! |
| Bean Validation | Request parameter validation | !!! |
| Springdoc OpenAPI | 3.0.3 | !!! |
| JUnit 5 | Unit and integration tests | !!! |
| Java concurrency APIs | `ExecutorService`, `BlockingQueue`, atomic types, locks | !!! |

## Swagger and OpenAPI

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Configured redirect: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

The Swagger index and OpenAPI JSON endpoints were verified against the running application.

## API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/simulate` | Runs expected, unsafe, and safe executions and returns their comparison. |
| `GET` | `/coins` | Returns the safe coin snapshots from the last completed simulation. |
| `GET` | `/stats` | Returns the statistics from the last completed simulation. |

### Simulation parameters

| Parameter | Required | Constraint | Description |
|---|---|---|---|
| `updates` | Yes | `1..100000` | Number of immutable update tasks. |
| `workers` | Yes | `1..16` | Fixed worker-pool size. |
| `seed` | No | `long` | Reproduces the same task list. A generated seed is returned when omitted. |

Example:

```powershell
curl.exe -X POST "http://localhost:8080/simulate?updates=10000&workers=4&seed=42"
```

### HTTP status codes

| Status | Situation |
|---:|---|
| `200` | Successful simulation or result lookup. |
| `400` | Missing or out-of-range request parameter. |
| `404` | No completed simulation exists for `/coins` or `/stats`. |
| `409` | Another simulation is already running. |
| `500` | Unexpected execution error. |

## Architecture

```text
POST /simulate
      |
      v
SimulationService
      |
      +--> TaskProducer --> one immutable PriceUpdateTask list
      |
      +--> ExpectedCalculator (single thread)
      |
      +--> SimulationEngine (UNSAFE)
      |
      +--> SimulationEngine (SAFE)
      |
      +--> invariant check --> immutable result snapshots
```

Each engine run creates its own coin states, counter, bounded queue, completion latch, and fixed worker pool:

```text
Producer --> ArrayBlockingQueue --> fixed PriceWorker pool --> coin state + counter
```

The queue capacity is `min(task count, 1000)`. Workers stop through one poison pill per worker. Completed statistics and safe snapshots are published through `AtomicReference`; an `AtomicBoolean` allows only one active simulation.

## Concurrency Concepts

- A fixed thread pool reuses a bounded number of worker threads instead of creating one thread per task. This limits thread creation, per-thread memory use, scheduling pressure, and context switching.
- `ArrayBlockingQueue.put()` applies backpressure when the queue is full. `take()` blocks idle workers without busy waiting.
- The unsafe counter uses a non-atomic read-modify-write increment, so concurrent updates may be lost.
- A coin update changes `currentPrice`, `updateCount`, `lastDelta`, and `lastUpdatedBy` as one logical operation. Protecting only one field would not make the complete state consistent.
- `CountDownLatch` waits for all real tasks. Poison pills stop queue consumers, and executor termination is awaited before result snapshots are created.
- Correctness is checked from the generated task list rather than inferred from timing or log output.

## Unsafe and Safe Implementations

| Concern | Unsafe path | Safe path |
|---|---|---|
| Processed counter | Plain `long` increment | `AtomicLong.incrementAndGet()` |
| Coin update | Unprotected compound update | Coin-level `ReentrantLock` |
| Expected result | Not applicable | Calculated sequentially from the same task list |
| Result validation | May deviate because of races | Checked with price, update-count, and processed-count invariants |

Unsafe output is not expected to fail on every run. Race conditions depend on thread scheduling; the implementation does not modify unsafe results artificially.

## Design Decisions

> !!! TEAM INPUT REQUIRED: Complete only the rationale agreed by the team.

| Decision | Current implementation | Team rationale |
|---|---|---|
| Task model | Immutable Java `record` | !!! |
| Workload reproducibility | `Random(seed)` | !!! |
| Shared workload | One `List.copyOf()` task list | !!! |
| Queue | Bounded `ArrayBlockingQueue` | !!! |
| Queue capacity | `min(task count, 1000)` | !!! |
| Worker execution | Fixed thread pool | !!! |
| Safe counter | `AtomicLong` | !!! |
| Safe coin state | One `ReentrantLock` per coin | !!! |
| Completion | `CountDownLatch` | !!! |
| Worker termination | Poison pills | !!! |
| Executor shutdown | `shutdown`, timed `awaitTermination`, then `shutdownNow` if needed | !!! |
| Active-run guard | `AtomicBoolean` | !!! |
| Result publication | `AtomicReference` with immutable snapshots | !!! |

## Correctness Invariants

Expected values are calculated sequentially from the same immutable task list used by both engine modes.

```text
safePrice = initialPrice + sum(all deltas generated for the coin)
safeUpdateCount = number of tasks generated for the coin
safeProcessedCount = submittedUpdates
```

The simulation reports `safeInvariantPassed=true` only when all three conditions hold.

## Race Condition Evidence

The recorded stress run used seed `42`, `50,000` updates, `4` workers, and `5` runs.

| Observation | Result |
|---|---:|
| Runs with unsafe counter deviation | 5 / 5 |
| Runs with unsafe coin deviation | 5 / 5 |
| Runs with successful safe invariant | 5 / 5 |
| Unsafe result artificially modified | No |

Detailed run data and analysis: [`docs/evidence/race-observation.md`](docs/evidence/race-observation.md)

## Performance Results

Recorded workload: seed `42`, `50,000` updates, task-level DEBUG logging disabled.

| Workers | Unsafe ms | Safe ms | Unsafe task/s | Safe task/s | Safe invariant |
|---:|---:|---:|---:|---:|---|
| 1 | 37 | 21 | 1,351,351 | 2,380,952 | Passed |
| 2 | 17 | 15 | 2,941,176 | 3,225,806 | Passed |
| 4 | 9 | 8 | 5,555,556 | 6,549,252 | Passed |
| 8 | 11 | 12 | 4,545,455 | 4,094,481 | Passed |

> !!! TEAM INPUT REQUIRED: Add the team's interpretation of the worker-count results, including task duration, context switching, and lock contention.

Full environment and result details: [`docs/evidence/benchmark-results.md`](docs/evidence/benchmark-results.md)

## ReentrantLock and synchronized

| Topic | `ReentrantLock` | `synchronized` | Team comment |
|---|---|---|---|
| Lock ownership | Explicit `lock()` / `unlock()` | Intrinsic monitor | !!! |
| Release discipline | `unlock()` in `finally` | Released automatically when leaving the monitor | !!! |
| Optional lock APIs | Supports APIs such as `tryLock()` and interruptible acquisition | Uses monitor entry | !!! |
| Project usage | One lock per `SafeCoinState` | Not used for safe coin updates | !!! |
| Team selection rationale | | | !!! |
| Coin-level versus global locking | | | !!! |

> !!! TEAM INPUT REQUIRED: Complete the comparison and the team's reason for selecting coin-level `ReentrantLock`.

## Thread Dump Analysis

The evidence dump captured four named safe workers while they were waiting for queue work.

```text
"safe-worker-1" ... WAITING (parking)
    at java.util.concurrent.ArrayBlockingQueue.take(...)
```

| Check | Observation |
|---|---|
| Requested workers | 4 |
| Visible safe workers | `safe-worker-1` through `safe-worker-4` |
| Worker state | 4 / 4 `WAITING (parking)` |
| Queue wait | `ArrayBlockingQueue.take()` visible for all four workers |
| `BLOCKED` workers | 0 |
| Java-level deadlock marker | Not present |
| Unbounded worker creation | Not observed |

- Raw dump: [`docs/evidence/thread-dump.txt`](docs/evidence/thread-dump.txt)
- Detailed analysis: [`docs/evidence/thread-dump-analysis.md`](docs/evidence/thread-dump-analysis.md)

## Testing

Run the default verification suite:

```powershell
mvn.cmd clean verify
```

Latest verified result on Java 21:

```text
Tests run: 53, Failures: 0, Errors: 0, Skipped: 3
BUILD SUCCESS
```

The three skipped tests are opt-in evidence harnesses:

```powershell
mvn.cmd "-DraceObservation=true" "-Dtest=RaceObservationEvidenceTest" test
mvn.cmd "-Dbenchmark=true" "-Dtest=SimulationBenchmarkTest" test
mvn.cmd "-DthreadDumpDemo=true" "-Dtest=ThreadDumpEvidenceTest" test
```

The test suite covers deterministic task generation, immutable task lists, queue behavior and backpressure, safe/unsafe counters and states, expected-value calculation, invariants, worker naming, completion, shutdown, validation, and controller integration.

## !!! Merge Conflict Experience

Verified Git history:

| Field | Value |
|---|---|
| First branch | `conflict/readme-memory-a` |
| Second branch | `conflict/readme-memory-b` |
| Conflicting file | `README.md` |
| First merged PR | [#33](https://github.com/ibrahimayhann/crypto-price-simulator/pull/33) |
| Conflict-resolution PR | [#32](https://github.com/ibrahimayhann/crypto-price-simulator/pull/32) |
| Conflict-resolution merge commit | `75f3037` |
| Resolution tool | !!! |
| Changes made by each branch | !!! |
| Content retained after resolution | !!! |
| Team lessons | !!! |

> !!! TEAM INPUT REQUIRED: Complete the unresolved fields using the team's actual conflict-resolution experience.

## Team Contributions

> !!! TEAM INPUT REQUIRED: Each member must verify their area.

| Member | Area |
|---|---|
| İbrahim | !!! |
| Fırat | !!! |
| Ahmet | !!! |
| Cem Bora | !!! |
| Tolga | !!! |

## !!! Bonus Work

> !!! TEAM INPUT REQUIRED: Document completed bonus work and its evidence. If no bonus was completed, replace this block with a clear statement that the submitted version contains no bonus work.

- Virtual Threads: !!!
- CompletableFuture: !!!
- Deadlock demonstration and lock ordering: !!!
