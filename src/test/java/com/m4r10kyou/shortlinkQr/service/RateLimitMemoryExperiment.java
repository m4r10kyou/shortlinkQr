package com.m4r10kyou.shortlinkQr.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LongSummaryStatistics;
import java.util.Map;


class RateLimitMemoryExperiment {

    private static final int N = 1_000_000;
    private static final int PASSES = 12;

    record RateLimitData(long count, long windowStart) {}

    @Test
    @Disabled("Manual experiment: Remove @Disabled to measure rate limit. Do not run in CI.")
    void rateLimitMemoryExperiment() {

        long[] bytesPerEntryPasses = new long[PASSES];

        System.out.println("=== Rate Limit Memory Experiment ===");
        System.out.println("Pass | Total Bytes | Bytes/IP");
        System.out.println("----------------------------------");

        for (int p = 0; p < PASSES; p++) {

            long memIpv4 = measureMapFootprint(true);
            long memIpv6 = measureMapFootprint(false);

            long maxPassMemory = Math.max(memIpv4, memIpv6);
            long bytesPerEntry = maxPassMemory / N;

            bytesPerEntryPasses[p] = bytesPerEntry;

            System.out.printf(" %02d  |  %10d |    %d%n", (p + 1), maxPassMemory, bytesPerEntry);
        }

        showResults(bytesPerEntryPasses);
    }

    private static long measureMapFootprint(boolean useIpv4) {

        forceGc();
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Size the map to avoid rehashing  during insertion
        Map<String, RateLimitData> map = new HashMap<>(N + (N / 3));

        for (int i = 0; i < N; i++) {

            map.put(useIpv4 ? buildIpv4(i) : buildIpv6(i), new RateLimitData(1, System.currentTimeMillis()));
        }

        forceGc();
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        // Keep a strong reference to the map until measurement is done
        if (map.size() != N) {
            throw new AssertionError("Map does not contain the expect number of references");
        }

        return memoryAfter - memoryBefore;
    }

    private static void forceGc() {

        System.gc();

        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String buildIpv4(int i) {
        //IPs like 10.x.y.z (14 chars)
        return String.format("10.%d.%d.%d", (i >> 16) & 255, (i >> 8) & 255, i & 255);
    }

    private static String buildIpv6(int i) {
        // IPs like 2001:0db8:85a3:0000:0000:8a2e:xxxx:yyyy (39 chars)
        return String.format("2001:0db8:85a3:0000:0000:8a2e:%04x:%04x", (i >> 16) & 0xFFFF, i & 0xFFFF);
    }

    private static void showResults(long[] bytesPerEntryPasses) {

        LongSummaryStatistics stats = Arrays.stream(bytesPerEntryPasses).summaryStatistics();
        System.out.println("----------------------------------");
        System.out.printf("Min: %d bytes/IP | Max: %d bytes/IP | Avg: %.0f bytes/IP%n",
                stats.getMin(), stats.getMax(), stats.getAverage());
        System.out.println("----------------------------------\n");
        System.out.println("Note: This value is a FLOOR. The final caching structure (like Caffeine)");
        System.out.println("will add its own overhead (e.g., expiration links) on top of this HashMap.");
    }
}
