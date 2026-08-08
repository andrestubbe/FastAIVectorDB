package demo;

import fastvectordb.FastVectorDB;
import fastvectordb.SearchResult;
import fastvectordb.VectorEntry;

import java.util.List;

public class Demo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== FastAIVectorDB Native SIMD Demo ===");

        try (FastVectorDB db = new FastVectorDB()) {
            System.out.println("Inserting sample vector embeddings into native store...");

            // Insert sample vector embeddings (384-dimensional or mini-vectors)
            db.insert(new VectorEntry(1, new float[]{0.85f, 0.12f, -0.45f, 0.91f}, "FastContentParse - High-speed document text extraction"));
            db.insert(new VectorEntry(2, new float[]{0.82f, 0.15f, -0.40f, 0.88f}, "FastContentChunk - AVX2 SIMD tokenization & Parent-Child chunking"));
            db.insert(new VectorEntry(3, new float[]{0.10f, 0.95f, 0.33f, -0.12f}, "FastAIVectorDB - Ultra-fast JNI vector database"));
            db.insert(new VectorEntry(4, new float[]{0.78f, 0.20f, -0.38f, 0.85f}, "FastAIRag - Zero-bloat Java RAG pipeline client"));

            // Query vector for Document Ingestion & Chunking similarity
            float[] queryVector = new float[]{0.84f, 0.14f, -0.42f, 0.90f};
            System.out.println("\nQuerying top 3 nearest vectors for SIMD Cosine Similarity...");

            long startTime = System.nanoTime();
            List<SearchResult> hits = db.search(queryVector, 3);
            long searchDurationNs = System.nanoTime() - startTime;

            System.out.printf("Search completed in %.3f µs (nanoseconds: %d)\n\n", searchDurationNs / 1000.0, searchDurationNs);
            System.out.println("Top Matches:");
            for (SearchResult hit : hits) {
                System.out.printf("  [ID: %d] Score: %.4f | Payload: %s\n",
                        hit.entry().id(), hit.score(), hit.entry().text());
            }
        }

        System.out.println("\n=== FastAIVectorDB Demo Complete ===");
    }
}
