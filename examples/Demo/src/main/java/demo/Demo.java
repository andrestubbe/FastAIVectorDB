package demo;

import fastaivectordb.FastVectorDB;
import fastaivectordb.SearchResult;
import fastaivectordb.VectorEntry;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Demo {

    private static final int DIMS = 128;
    private static final Random RNG = new Random(42);

    public static void main(String[] args) throws Exception {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        System.out.println("=== FastAIVectorDB Native SIMD Demo ===\n");

        // ── Phase 1: Bulk Insert ──────────────────────────────────
        int insertCount = 10_000;
        System.out.println("Phase 1  Bulk Insert — " + insertCount + " vectors (" + DIMS + " dims)\n");

        try (FastVectorDB db = new FastVectorDB()) {

            long insertStart = System.nanoTime();
            for (int i = 0; i < insertCount; i++) {
                db.insert(new VectorEntry(i, randomVec(), "vec-" + i));
                printProgress(i + 1, insertCount);
            }
            long insertMs = (System.nanoTime() - insertStart) / 1_000_000;
            long vecsPerSec = insertCount * 1000L / Math.max(insertMs, 1);
            System.out.printf("%n%nInserted %,d vectors in %d ms  (%,d vec/sec)%n", insertCount, insertMs, vecsPerSec);

            // ── Phase 2: Scale Benchmark ──────────────────────────
            System.out.println("\n----------------------------------------------------------");
            System.out.println("Phase 2  SIMD Cosine Search — scaling across index sizes\n");

            int[] scales = {100, 1_000, 10_000};
            float[] queryVec = randomVec();

            for (int scale : scales) {
                // Search on a subset by using a fresh db of exactly `scale` entries
                try (FastVectorDB scaled = new FastVectorDB()) {
                    for (int i = 0; i < scale; i++) {
                        scaled.insert(new VectorEntry(i, randomVec(), ""));
                    }
                    // Warm-up
                    scaled.search(queryVec, 3);

                    // Measure average over 10 runs
                    long total = 0;
                    int runs = 10;
                    for (int r = 0; r < runs; r++) {
                        long t0 = System.nanoTime();
                        scaled.search(queryVec, 3);
                        total += System.nanoTime() - t0;
                    }
                    double avgUs = (total / runs) / 1000.0;
                    System.out.printf("  %,6d vectors  ->  %8.1f µs%n", scale, avgUs);
                }
            }

            System.out.println("\n  Native SIMD (AVX2) cosine search — scales linearly with index size.");
            System.out.println("----------------------------------------------------------");
        }

        System.out.println("\n=== FastAIVectorDB Demo Complete ===");
    }

    private static void printProgress(int done, int total) {
        int width = 30;
        int filled = (int) ((double) done / total * width);
        int percent = (int) ((double) done / total * 100);
        StringBuilder sb = new StringBuilder("\r  ");
        for (int i = 0; i < width; i++) sb.append(i < filled ? "█" : "░");
        sb.append(String.format("  %3d%%  (%,d / %,d)", percent, done, total));
        System.out.print(sb);
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
