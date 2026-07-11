package fastaivectordb;

import java.util.List;

public interface VectorStore extends AutoCloseable {

    void insert(VectorEntry entry);

    List<SearchResult> search(float[] query, int k);

    int size();

    void clear();

    void save(String path);

    void load(String path);

    @Override
    default void close() throws Exception {
    }
}
