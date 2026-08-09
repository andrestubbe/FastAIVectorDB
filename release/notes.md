## What's New in 0.1.2

### Pluggable Embedder API
- New `Embedder` interface — swap any embedding model in one line
- `Embedder.onnx(modelPath, tokenizerPath)` factory for ONNX models
- `OnnxEmbedder` built-in: BGE-Micro-v2, E5-Small, any HuggingFace ONNX model
- `Embedder` extends `AutoCloseable` for safe try-with-resources usage
- `onnxruntime 1.18.0` + `djl tokenizers 0.28.0` as transitive dependencies

### Usage
```java
try (Embedder embedder = Embedder.onnx("models/bge-micro-v2.onnx", "models/tokenizer.json")) {
    float[] vec = embedder.embed("Der Haushaltsplan wird durch Gesetz festgestellt.");
}
```
