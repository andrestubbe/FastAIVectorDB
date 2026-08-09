# FastAIVectorDB Changelog

## [0.1.2] - 2026-08-09
- Added `Embedder` interface — pluggable, swappable embedding strategy.
- Added `OnnxEmbedder` — ONNX Runtime + DJL Tokenizer integration (BGE-Micro-v2, E5-Small, etc.).
- Added `Embedder.onnx(modelPath, tokenizerPath)` factory method.
- `Embedder` extends `AutoCloseable` — safe for try-with-resources.
- `onnxruntime 1.18.0` + `djl tokenizers 0.28.0` added as transitive dependencies.

## [0.1.1] - 2026-08-08
- Persistence support: `save()` and `load()` for native vector index + metadata.
- `textMap` metadata persistence via parallel `.meta` binary file.
- JitPack release.

## [0.1.0] - 2026-08-08
- Initial high-performance native vector database release.
- Native C++ SIMD cosine similarity search engine.
- Pure Java `InMemoryVectorStore` fallback.
- FastCore JNI native library loader integration.
- JMH performance benchmarking suite.
