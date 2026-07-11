package fastaivectordb;

import java.util.ArrayList;
import java.util.List;

/**
 * Thread-safe, pure-Java vector store.
 * Uses linear scan with cosine similarity — no native dependencies.
 * Suitable for testing and small datasets (< 100k vectors).
 */
public final class InMemoryVectorStore implements VectorStore {

    private final List<VectorEntry> entries = new ArrayList<>();

    @Override
    public synchronized void insert(VectorEntry entry) {
        entries.add(entry);
    }

    @Override
    public synchronized List<SearchResult> search(float[] query, int k) {
        List<SearchResult> results = new ArrayList<>(entries.size());
        for (VectorEntry entry : entries) {
            float score = cosineSimilarity(query, entry.vector());
            results.add(new SearchResult(entry, score));
        }
        results.sort((a, b) -> Float.compare(b.score(), a.score()));
        return results.subList(0, Math.min(k, results.size()));
    }

    @Override
    public synchronized int size() {
        return entries.size();
    }

    @Override
    public synchronized void clear() {
        entries.clear();
    }

    private static float cosineSimilarity(float[] a, float[] b) {
        int len = Math.min(a.length, b.length);
        float dot = 0f, normA = 0f, normB = 0f;
        for (int i = 0; i < len; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        float denom = (float) (Math.sqrt(normA) * Math.sqrt(normB));
        return denom == 0f ? 0f : dot / denom;
    }
}
