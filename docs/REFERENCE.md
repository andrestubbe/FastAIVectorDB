# FastAIVectorDB API Reference

## Classes

### `VectorEntry`
A record representing a vector entry inside the DB.
```java
public record VectorEntry(int id, float[] vector, String text) {}
```

### `SearchResult`
A search hit containing the original entry and the calculated similarity score.
```java
public record SearchResult(VectorEntry entry, float score) {}
```

### `FastVectorDB`
The main native/fallback database wrapper implementing `VectorStore`.
```java
public final class FastVectorDB implements VectorStore {
    public FastVectorDB();
    public void insert(VectorEntry entry);
    public List<SearchResult> search(float[] query, int k);
    public int size();
    public void clear();
    public void close();
}
```
