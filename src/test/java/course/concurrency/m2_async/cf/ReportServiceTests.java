package course.concurrency.m2_async.cf;

import course.concurrency.m2_async.cf.report.ReportServiceCF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class ReportServiceTests {

    private final Map<String, ExecutorFactory> executorFactoriesPart1 = Map.of(
            "ForkJoinPool.commonPool()", ForkJoinPool::commonPool,
            "Executors.newCachedThreadPool()", Executors::newCachedThreadPool,
            "Executors.newFixedThreadPool(4)", () -> Executors.newFixedThreadPool(4),
            "Executors.newFixedThreadPool(8)", () -> Executors.newFixedThreadPool(8),
            "Executors.newFixedThreadPool(16)", () -> Executors.newFixedThreadPool(16),
            "Executors.newFixedThreadPool(24)", () -> Executors.newFixedThreadPool(24),
            "Executors.newFixedThreadPool(32)", () -> Executors.newFixedThreadPool(32),
            "Executors.newFixedThreadPool(64)", () -> Executors.newFixedThreadPool(64),
            "Executors.newFixedThreadPool(128)", () -> Executors.newFixedThreadPool(128),
            "Executors.newFixedThreadPool(256)", () -> Executors.newFixedThreadPool(256)
    );
    private final Map<String, ExecutorFactory> executorFactoriesPart2 = Map.of(
            "Executors.newWorkStealingPool(4)", () -> Executors.newWorkStealingPool(4),
            "Executors.newWorkStealingPool()", Executors::newWorkStealingPool,
            "Executors.newWorkStealingPool(24) ", () -> Executors.newWorkStealingPool(24),
            "Executors.newWorkStealingPool(32) ", () -> Executors.newWorkStealingPool(32),
            "Executors.newWorkStealingPool(64)", () -> Executors.newWorkStealingPool(64),
            "Executors.newWorkStealingPool(128)", () -> Executors.newWorkStealingPool(128)
    );

    private static long getExecutionTime(ReportServiceCF reportService) {
        final int poolSize = Runtime.getRuntime().availableProcessors() * 3;
        final int iterations = 5;

        final CountDownLatch latch = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (int i = 0; i < poolSize; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException ignored) {
                }
                for (int it = 0; it < iterations; it++) {
                    reportService.getReport();
                }
            });
        }

        final long start = System.currentTimeMillis();
        latch.countDown();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
                System.out.println("Timeout elapsed!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
        final long end = System.currentTimeMillis();

        return end - start;
    }

    @Test
    void testExecutionReport() {
        final Map<String, ExecutorFactory> executorFactories = new HashMap<>();
        executorFactories.putAll(executorFactoriesPart1);
        executorFactories.putAll(executorFactoriesPart2);

        final Map<String, List<Long>> data = new HashMap<>();
        final var reportBenchmark = new ReportBenchmark(data);

        final int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            System.out.println("Iteration " + (i + 1) + " of " + iterations);
            executorFactories.forEach(reportBenchmark);
        }

        System.out.println("----- REPORT BEGIN -----");
        data.forEach((executorType, longs) -> {
            final long min = Collections.min(longs);
            final long max = Collections.max(longs);
            System.out.println("|" + executorType + "|compute|" + min + "|" + max);
        });
        System.out.println("----- REPORT END -----");
    }

    @Test
    @Disabled
    public void testMultipleTasks() {
        final long executionTime = getExecutionTime(new ReportServiceCF(ForkJoinPool.commonPool()));
        System.out.println("Execution time: " + executionTime);
    }

    @FunctionalInterface
    interface ExecutorFactory {
        ExecutorService create();
    }

    private static class ReportBenchmark implements BiConsumer<String, ExecutorFactory> {
        private final Map<String, List<Long>> data;

        public ReportBenchmark(Map<String, List<Long>> data) {
            this.data = data;
        }

        @Override
        public void accept(String executorType, ExecutorFactory executorFactory) {
            final long executionTime = getExecutionTime(new ReportServiceCF(executorFactory.create()));
            System.out.println("" + executorType + " execution time: " + executionTime);
            final List<Long> dataItem = data.get(executorType);
            if (dataItem != null) {
                dataItem.add(executionTime);
            } else {
                var newItem = new ArrayList<Long>();
                newItem.add(executionTime);
                data.put(executorType, newItem);
            }
        }
    }
}
