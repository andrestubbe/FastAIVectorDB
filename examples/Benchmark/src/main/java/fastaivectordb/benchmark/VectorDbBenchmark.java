package fastaivectordb.benchmark;

import fastaivectordb.FastVectorDB;
import fastaivectordb.SearchResult;
import fastaivectordb.VectorEntry;
import org.openjdk.jmh.annotations.*;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class VectorDbBenchmark {

    private FastVectorDB db;
    private float[] queryVector;

    @Setup(Level.Trial)
    public void setup() {
        db = new FastVectorDB();
        Random rng = new Random(42);
        int dim = 384; // Standard MiniLM embedding size

        // Populate database with 10,000 vector entries
        for (int i = 0; i < 10000; i++) {
            float[] vec = new float[dim];
            for (int d = 0; d < dim; d++) {
                vec[d] = rng.nextFloat() * 2.0f - 1.0f;
            }
            db.insert(new VectorEntry(i, vec, "Sample passage text #" + i));
        }

        queryVector = new float[dim];
        for (int d = 0; d < dim; d++) {
            queryVector[d] = rng.nextFloat() * 2.0f - 1.0f;
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Benchmark
    public List<SearchResult> benchmarkVectorSimilaritySearch() {
        return db.search(queryVector, 5);
    }
}
