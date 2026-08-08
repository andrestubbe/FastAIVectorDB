# FastAIVectorDB Reference Manual

## Core API

### `FastVectorDB`
Main entry point for similarity search and vector persistence.

```java
try (FastVectorDB db = new FastVectorDB()) {
    db.insert(new VectorEntry(id, embedding, text));
    List<SearchResult> hits = db.search(queryVector, topK);
}
```

### `VectorEntry`
Record holding vector payload:
* `id` (long): Unique vector identifier.
* `vector` (float[]): Feature embedding array.
* `text` (String): Associated text snippet or parent context reference.

### `SearchResult`
Result record containing matched `VectorEntry` and cosine similarity `score` (float).
