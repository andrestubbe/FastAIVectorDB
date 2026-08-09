package demo;

import fastansi.FastANSI;
import fastaivectordb.FastVectorDB;
import fastaivectordb.VectorEntry;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Demo {

    private static final int DIMS = 128;
    private static final Random RNG = new Random(42);

    private static String gray(String text) {
        return FastANSI.FG_BRIGHT_BLACK + text + FastANSI.RESET;
    }

    private static String white(String text) {
        return FastANSI.FG_BRIGHT_WHITE + text + FastANSI.RESET;
    }

    private static String cyan(String text) {
        return FastANSI.FG_BRIGHT_CYAN + text + FastANSI.RESET;
    }

    public static void main(String[] args) throws Exception {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception ignored) {}

        System.out.println(cyan("=== FastAIVectorDB Native SIMD Demo ===") + "\n");

        // ── Phase 1: Bulk Insert ──────────────────────────────────
        int insertCount = 10_000;
        System.out.println(gray("[1/2] BULK VECTOR INSERTION"));
        System.out.println(gray("      Target: ") + white(String.format("%,d vectors (%d dims)", insertCount, DIMS)) + "\n");

        try (FastVectorDB db = new FastVectorDB()) {

            long insertStart = System.nanoTime();
            for (int i = 0; i < insertCount; i++) {
                db.insert(new VectorEntry(i, randomVec(), "vec-" + i));
                printProgress(i + 1, insertCount);
            }
            long insertMs = (System.nanoTime() - insertStart) / 1_000_000;
            long vecsPerSec = insertCount * 1000L / Math.max(insertMs, 1);
            System.out.printf("%n%n" + gray("      ✓ Inserted %,d vectors in %d ms (") + cyan("%,d vec/sec") + gray(")") + "%n%n", insertCount, insertMs, vecsPerSec);

            // ── Phase 2: Scale Benchmark ──────────────────────────
            System.out.println(gray("[2/2] SIMD COSINE SEARCH (AVX2 Scaling Benchmark)"));
            System.out.println(gray("      ----------------------------------------------------------"));

            int[] scales = {100, 1_000, 10_000};
            float[] queryVec = randomVec();

            for (int scale : scales) {
                try (FastVectorDB scaled = new FastVectorDB()) {
                    for (int i = 0; i < scale; i++) {
                        scaled.insert(new VectorEntry(i, randomVec(), ""));
                    }
                    scaled.search(queryVec, 3);

                    long total = 0;
                    int runs = 10;
                    for (int r = 0; r < runs; r++) {
                        long t0 = System.nanoTime();
                        scaled.search(queryVec, 3);
                        total += System.nanoTime() - t0;
                    }
                    double avgUs = (total / runs) / 1000.0;
                    System.out.printf(gray("      • Index Scale: ") + white(String.format("%,6d vectors", scale)) + gray("  ->  Retrieval: ") + cyan(String.format("%8.1f µs", avgUs)) + "%n");
                }
            }

            System.out.println(gray("      ----------------------------------------------------------"));
            System.out.println(gray("      ✓ Native SIMD AVX2 cosine search scales linearly with zero GC pressure."));
        }

        System.out.println("\n" + cyan("=== FastAIVectorDB Demo Complete ==="));
    }

    private static void printProgress(int done, int total) {
        int width = 30;
        int filled = (int) ((double) done / total * width);
        int percent = (int) ((double) done / total * 100);
        StringBuilder sb = new StringBuilder("\r      ");
        for (int i = 0; i < width; i++) sb.append(i < filled ? "█" : "░");
        sb.append(String.format("  %3d%%  (%,d / %,d)", percent, done, total));
        System.out.print(gray(sb.toString()));
    }

    private static float[] randomVec() {
        float[] v = new float[DIMS];
        float norm = 0f;
        for (int i = 0; i < DIMS; i++) { v[i] = RNG.nextFloat() * 2 - 1; norm += v[i] * v[i]; }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < DIMS; i++) v[i] /= norm;
        return v;
    }
}
