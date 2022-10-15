package course.concurrency.m2_async.cf;

import course.concurrency.m2_async.cf.report.ReportServiceCF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;
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
            "Executors.newWorkStealingPool(64) ", () -> Executors.newWorkStealingPool(64),
            "Executors.newWorkStealingPool(128) ", () -> Executors.newWorkStealingPool(128)
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
        final TreeMap<Long, String> executionReport = new TreeMap<>();
        final var reportBenchmark = new ReportBenchmark(executionReport);
        executorFactoriesPart1.forEach(reportBenchmark);
        executorFactoriesPart2.forEach(reportBenchmark);
        System.out.println("***** REPORT BEGIN *****");
        executionReport.forEach((ignored, s) -> System.out.println(s));
        System.out.println("***** REPORT END *****");
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
        private final TreeMap<Long, String> executionReport;

        public ReportBenchmark(TreeMap<Long, String> executionReport) {
            this.executionReport = executionReport;
        }

        @Override
        public void accept(String executorType, ExecutorFactory executorFactory) {
            final long executionTime = getExecutionTime(new ReportServiceCF(executorFactory.create()));
            final String reportString = "| " + executorType + " | compute  | " + executionTime + "|";
            System.out.println(reportString);
            executionReport.put(executionTime, reportString);
        }
    }
}
