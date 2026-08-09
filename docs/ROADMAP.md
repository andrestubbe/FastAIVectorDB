# FastAIVectorDB Roadmap

- [x] Initial C++ SIMD native vector similarity engine.
- [x] Pure Java fallback implementation.
- [x] Pluggable `Embedder` interface + `OnnxEmbedder` (ONNX Runtime + DJL Tokenizer).
- [ ] AVX-512 vector distance optimizations.
- [ ] Disk-backed HNSW (Hierarchical Navigable Small World) index support.
- [ ] Cross-platform Linux (.so) and macOS (.dylib) native binaries.
