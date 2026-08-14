package fastaivectordb.benchmark;

import fastaivectordb.FastVectorDB;
import fastaivectordb.VectorEntry;
import org.openjdk.jmh.annotations.*;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 2, time = 1)
@Fork(1)
public class JMH_VectorDB {

    private FastVectorDB db;
    private float[] queryVector;

    @Setup
    public void setup() {
        db = new FastVectorDB();
        Random rand = new Random(42);
        int dim = 128;
        
        for (int i = 0; i < 1000; i++) {
            float[] vec = new float[dim];
            for (int d = 0; d < dim; d++) {
                vec[d] = rand.nextFloat();
            }
            db.insert(new VectorEntry(i, vec, "doc_" + i));
        }

        queryVector = new float[dim];
        for (int d = 0; d < dim; d++) {
            queryVector[d] = rand.nextFloat();
        }
    }

    @TearDown
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Benchmark
    public Object benchmarkAVX2CosineSearch() {
        return db.search(queryVector, 10);
    }
}
