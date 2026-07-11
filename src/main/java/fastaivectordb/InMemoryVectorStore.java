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

    @Override
    public synchronized void save(String path) {
        try (java.io.DataOutputStream out = new java.io.DataOutputStream(new java.io.FileOutputStream(path))) {
            out.writeInt(entries.size());
            for (VectorEntry e : entries) {
                out.writeInt(e.id());
                out.writeUTF(e.text());
                out.writeInt(e.vector().length);
                for (float f : e.vector()) {
                    out.writeFloat(f);
                }
            }
        } catch (java.io.IOException ex) {
            throw new RuntimeException("Failed to save InMemoryVectorStore: " + ex.getMessage(), ex);
        }
    }

    @Override
    public synchronized void load(String path) {
        if (!new java.io.File(path).exists()) return;
        try (java.io.DataInputStream in = new java.io.DataInputStream(new java.io.FileInputStream(path))) {
            entries.clear();
            int count = in.readInt();
            for (int i = 0; i < count; i++) {
                int id = in.readInt();
                String text = in.readUTF();
                int len = in.readInt();
                float[] vec = new float[len];
                for (int j = 0; j < len; j++) {
                    vec[j] = in.readFloat();
                }
                entries.add(new VectorEntry(id, vec, text));
            }
        } catch (java.io.IOException ex) {
            throw new RuntimeException("Failed to load InMemoryVectorStore: " + ex.getMessage(), ex);
        }
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
