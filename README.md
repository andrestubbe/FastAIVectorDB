# FastAIVectorDB

Ultrafast in-memory vector database with JNI native backend for the FastJava AI ecosystem.

## Performance-First Design
FastAIVectorDB provides high-performance vector search:
- **JNI-first backend**: Math is executed directly in C++ for maximum throughput.
- **Pure-Java Fallback**: Automatically transparently falls back to `InMemoryVectorStore` if the native DLL is not loaded.
- **Thread-safe**: Designed for concurrent retrieval pipelines.
