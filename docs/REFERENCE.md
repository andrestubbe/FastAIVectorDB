# FastAIVectorDB Reference Manual

## `Embedder` — Pluggable Embedding Interface

```java
// Create an ONNX embedder (swap model path to change model)
Embedder embedder = Embedder.onnx("models/bge-micro-v2.onnx", "models/tokenizer.json");
float[] vec = embedder.embed("text to embed");
embedder.close();

// Or as lambda (no-op close):
Embedder custom = text -> myModel.embed(text);
```

| Method | Description |
|--------|-------------|
| `Embedder.onnx(Path, Path)` | Factory: loads ONNX model + HuggingFace tokenizer |
| `Embedder.onnx(String, String)` | String path convenience overload |
| `embed(String)` | Returns L2-normalized `float[]` |
| `close()` | Releases ONNX session and tokenizer (default no-op for lambdas) |

## `OnnxEmbedder` — ONNX Embedding Implementation

Internal implementation of `Embedder`. Performs:
1. HuggingFace tokenization (DJL `tokenizer.json`)
2. ONNX Runtime inference (`last_hidden_state`)
3. Mean-pooling over token dimension (masked by attention mask)
4. L2-normalization to unit vector

Use via `Embedder.onnx()` factory — do not instantiate directly.

## `FastVectorDB` — Native SIMD Vector Store

```java
try (FastVectorDB db = new FastVectorDB()) {
    db.insert(new VectorEntry(id, embedding, text));
    List<SearchResult> hits = db.search(queryVector, topK);
}
```

### `VectorEntry`
Record holding vector payload:
* `id` (int): Unique vector identifier.
* `vector` (float[]): Feature embedding array.
* `text` (String): Associated text snippet or parent context reference.

### `SearchResult`
Result record containing matched `VectorEntry` and cosine similarity `score` (float).
