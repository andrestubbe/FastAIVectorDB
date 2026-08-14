# FastAIVectorDB 0.1.4 — Ultrafast Native Vector Database for Java

[![Status](https://img.shields.io/badge/status-0.1.2-brightgreen.svg)](https://github.com/andrestubbe/FastAIVectorDB/releases/tag/0.1.2)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-0.1.2-green.svg)](https://jitpack.io/#andrestubbe/FastAIVectorDB)

---

**⚡ Raw JNI performance with pure Java fallbacks — Zero allocation vector database built for high-throughput JVM environments.**

FastAIVectorDB is a **minimalist, hyper-fast JNI vector store** tailored for developers who need maximum similarity lookup performance without running heavy Python processes, Docker instances, or bloated database setups. It is designed to work alongside **[FastContentParse](https://github.com/andrestubbe/FastContentParse)**, **[FastContentChunk](https://github.com/andrestubbe/FastContentChunk)**, and **[FastAIRag](https://github.com/andrestubbe/FastAIRag)** to accelerate vector search and Parent-Child context retention.

[![Showcase](docs/screenshot.png)](https://youtu.be/ad32xjTpmvk)

---

## Quick Start — With Built-in ONNX Embedder

```java
import fastaivectordb.Embedder;
import fastaivectordb.FastVectorDB;
import fastaivectordb.VectorEntry;
import fastaivectordb.SearchResult;

public class Demo {
    public static void main(String[] args) {
        // Load any ONNX embedding model — swap model path to change model
        try (Embedder embedder = Embedder.onnx("models/bge-micro-v2.onnx", "models/tokenizer.json");
             FastVectorDB db  = new FastVectorDB()) {

            // 1. Embed and insert a document chunk
            float[] vec = embedder.embed("Der Haushaltsplan wird durch Gesetz festgestellt.");
            db.insert(new VectorEntry(0, vec, "§ 1 BHO"));

            // 2. Embed the query and search
            float[] queryVec = embedder.embed("Wie wird der Haushaltsplan festgestellt?");
            List<SearchResult> hits = db.search(queryVec, 1);

            // 3. Inspect result
            hits.forEach(h -> System.out.printf("ID: %d | Score: %.4f | %s%n",
                h.entry().id(), h.score(), h.entry().text()));
        }
    }
}
```

## Quick Start — Raw Vectors (no embedder)

```java
try (FastVectorDB db = new FastVectorDB()) {
    db.insert(new VectorEntry(0, new float[]{0.1f, -0.2f, 0.89f}, "Document snippet"));
    List<SearchResult> hits = db.search(new float[]{0.1f, -0.1f, 0.9f}, 5);
    hits.forEach(h -> System.out.printf("ID: %d | Score: %.4f | %s%n",
        h.entry().id(), h.score(), h.entry().text()));
}
```

---

## Table of Contents

- [Why FastAIVectorDB?](#why-fastaivectordb)
- [Key Features](#key-features)
- [Performance Benchmarks](#performance-benchmarks)
- [Architecture Overview](#architecture-overview)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Documentation](#documentation)
- [Platform Support](#platform-support)
- [License](#license)
- [Related Projects](#related-projects)

---

## Why FastAIVectorDB?

Traditional vector databases force developers to run external Docker containers, Python bridges, or heavy network daemons that introduce 20-50ms latency spikes per query. `FastAIVectorDB` solves this by providing:

- **Embedded Sub-Millisecond Search** — Scans vector databases directly within the JVM process.
- **Native SIMD Acceleration** — Uses compiled C++ AVX/SSE vector instructions for fast cosine similarity.
- **Zero GC Allocations** — Direct memory mappings that prevent JVM garbage collector pauses during large vector scans.
- **Pure-Java Fallback** — Thread-safe `InMemoryVectorStore` fallback if native binaries are restricted.

---

## Key Features

* **🚀 Native SIMD Performance** — Highly optimized vector similarity operations written in C++ linked via JNI.
* **🧩 Pluggable Embedder API** — Built-in `Embedder` interface with `Embedder.onnx()` factory: swap any ONNX model in one line.
* **🤖 ONNX Embedding Support** — Ships with `OnnxEmbedder` (BGE-Micro-v2, E5-Small, etc.) via ONNX Runtime + DJL Tokenizer.
* **🛡️ Pure-Java Fallback** — Instant, automatic fallback to a thread-safe `InMemoryVectorStore` if native DLL is missing.
* **⚡ Zero Memory Overhead** — Direct memory mappings preventing garbage collector stalls on vector queries.
* **🧠 Parent-Child Vector Payload** — Retains both small `chunk.text` for vector indexing and rich `chunk.parentText` for LLM context.

---

## Performance Benchmarks

`FastAIVectorDB` is built for low-latency similarity search across thousands of embeddings. In the official [JMH Benchmark](examples/Benchmark), the system measured k-NN scan throughput over 10,000 vectors (384 dimensions):

```text
Benchmark                                           Mode  Cnt  Score   Error   Units
VectorDbBenchmark.benchmarkVectorSimilaritySearch  thrpt    5  81.0    ± 0.05  ops/ms
```

> **81,000 Vector Scans per Second**: `FastAIVectorDB` evaluates k-Nearest Neighbors cosine similarity across 10,000 active embeddings in **under 12 microseconds per query** (81 ops/ms).

---

## Architecture Overview

**[FastContentParse](https://github.com/andrestubbe/FastContentParse) (The Parser)**  
Converts unstructured binary documents (PDF, RTF, Markdown, TXT) into normalized UTF-8 text streams.

**[FastContentChunk](https://github.com/andrestubbe/FastContentChunk) (The Strategy Engine)**  
Segments normalized text streams into contextual passages with Parent-Child context.

**FastAIVectorDB (This Library — The Vector Store)**  
High-speed native C++ SIMD vector database storing small `chunk.text` embeddings for sub-5ms similarity retrieval.

**[FastAIRag](https://github.com/andrestubbe/FastAIRag) (The Orchestration Pipeline)**  
Higher-level RAG framework that orchestrates **[FastContentParse](https://github.com/andrestubbe/FastContentParse)** and **[FastContentChunk](https://github.com/andrestubbe/FastContentChunk)**, indexes small `chunk.text` embeddings into **[FastAIVectorDB](https://github.com/andrestubbe/FastAIVectorDB)**, and feeds `chunk.parentText` to **[FastAIBot](https://github.com/andrestubbe/FastAIBot)** for LLM response generation.

---

## API Quick Reference

### `Embedder` — Pluggable Embedding Interface

| Method | Description |
|--------|-------------|
| `Embedder.onnx(modelPath, tokenizerPath)` | Creates an ONNX embedder (BGE-Micro-v2, E5-Small, …) |
| `embed(String text)` | Returns L2-normalized `float[]` vector |
| `close()` | Releases ONNX session and tokenizer resources |

### `FastVectorDB` — SIMD Vector Store

| Method | Description |
|--------|-------------|
| `insert(VectorEntry)` | Inserts a vector entry with ID, float[] embedding, and payload. |
| `search(float[], int)` | Scans database for top-K cosine similarity matches. |
| `close()` | Releases native memory allocations and flushes indexes. |

---

## Recommended ONNX Embedding Models

Download any of these models from HuggingFace and pass the paths to `Embedder.onnx()`:

| Model | Size | Dimensions | HuggingFace Link |
|-------|------|-----------|-----------------|
| **BGE-Micro-v2** ⭐ Recommended | ~23 MB | 384 | [TaylorAI/bge-micro-v2](https://huggingface.co/TaylorAI/bge-micro-v2/tree/main/onnx) |
| **BGE-Small-EN-v1.5** | ~130 MB | 384 | [BAAI/bge-small-en-v1.5](https://huggingface.co/BAAI/bge-small-en-v1.5/tree/main/onnx) |
| **E5-Small-v2** | ~130 MB | 384 | [intfloat/e5-small-v2](https://huggingface.co/intfloat/e5-small-v2/tree/main/onnx) |
| **BGE-Base-EN-v1.5** | ~440 MB | 768 | [BAAI/bge-base-en-v1.5](https://huggingface.co/BAAI/bge-base-en-v1.5/tree/main/onnx) |

### How to Download (PowerShell)

```powershell
# Download BGE-Micro-v2 (recommended, ~23 MB)
New-Item -ItemType Directory -Force -Path models
Invoke-WebRequest -Uri "https://huggingface.co/TaylorAI/bge-micro-v2/resolve/main/onnx/model.onnx" `
    -OutFile "models/bge-micro-v2.onnx" `
    -Headers @{Authorization="Bearer hf_YOUR_TOKEN"} -MaximumRedirection 10
Invoke-WebRequest -Uri "https://huggingface.co/TaylorAI/bge-micro-v2/resolve/main/tokenizer.json" `
    -OutFile "models/tokenizer.json" `
    -Headers @{Authorization="Bearer hf_YOUR_TOKEN"} -MaximumRedirection 10
```

> [!NOTE]
> A free HuggingFace account and Read token are required. Create one at [huggingface.co/settings/tokens](https://huggingface.co/settings/tokens).

---

## Installation

### Option 1: Maven (Recommended)

Add the JitPack repository and the complete dependency stack to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <!-- FastAIVectorDB Vector Store -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastAIVectorDB</artifactId>
        <version>0.1.4</version>
    </dependency>

    <!-- FastSIMD Hardware Vector Acceleration Engine -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastSIMD</artifactId>
        <version>0.1.3</version>
    </dependency>

    <!-- FastMemory Aligned Allocator -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastMemory</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastPointer Address Wrapper -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastPointer</artifactId>
        <version>0.1.1</version>
    </dependency>

    <!-- FastCore Native Loader -->
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.0</version>
    </dependency>

    <!-- FastAIModel ONNX Module -->
    <dependency>
        <groupId>com.github.andrestubbe.FastAIModel</groupId>
        <artifactId>fastaimodel-onnx</artifactId>
        <version>0.1.2</version>
    </dependency>

    <!-- ONNX Runtime for Local Embeddings -->
    <dependency>
        <groupId>com.microsoft.onnxruntime</groupId>
        <artifactId>onnxruntime</artifactId>
        <version>1.18.0</version>
    </dependency>

    <!-- DJL Tokenizer for HuggingFace Models -->
    <dependency>
        <groupId>ai.djl.huggingface</groupId>
        <artifactId>tokenizers</artifactId>
        <version>0.28.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastAIVectorDB:0.1.4'
    implementation 'com.github.andrestubbe:FastSIMD:0.1.3'
    implementation 'com.github.andrestubbe:FastMemory:0.1.1'
    implementation 'com.github.andrestubbe:FastPointer:0.1.1'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
    implementation 'com.github.andrestubbe.FastAIModel:fastaimodel-onnx:0.1.2'
    implementation 'com.microsoft.onnxruntime:onnxruntime:1.18.0'
    implementation 'ai.djl.huggingface:tokenizers:0.28.0'
}
```

### Option 3: Direct Download (No Build Tool)

Download the latest JARs directly to add them to your classpath:

1. ⚡ **[FastAIVectorDB-0.1.4.jar](https://github.com/andrestubbe/FastAIVectorDB/releases/download/0.1.4/FastAIVectorDB-0.1.4.jar)** (SIMD Vector Store Engine)
2. 🚀 **[FastSIMD-0.1.3.jar](https://github.com/andrestubbe/FastSIMD/releases/download/0.1.3/FastSIMD-0.1.3.jar)** (Hardware Vector Acceleration Engine)
3. 💾 **[FastMemory-0.1.1.jar](https://github.com/andrestubbe/FastMemory/releases/download/0.1.1/FastMemory-0.1.1.jar)** (32-Byte Aligned Allocator)
4. 📍 **[FastPointer-0.1.1.jar](https://github.com/andrestubbe/FastPointer/releases/download/0.1.1/FastPointer-0.1.1.jar)** (Primitive Address Wrapper)
5. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (Mandatory Native Loader)

> [!IMPORTANT]
> All JARs must be included in your classpath for the native JNI vector search bindings to function correctly.

---

## Documentation

* **[REFERENCE.md](docs/REFERENCE.md)**: Core API reference manual.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Zero-allocation vector architecture design goals.
* **[COMPILE.md](docs/COMPILE.md)**: Native C++ build instructions.
* **[CHANGELOG.md](docs/CHANGELOG.md)**: Project history.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future development goals.

---

## Platform Support

| Platform | Status |
|----------|--------|
| Windows 10/11 (x64) | ✅ Fully Supported |
| Linux | 🚧 Planned |
| macOS | 🚧 Planned |

---

## License

MIT License — See [LICENSE](LICENSE) file for details.

---

## Related Projects

- [FastContentParse](https://github.com/andrestubbe/FastContentParse) — Standardized Java document parser for text extraction and normalization
- [FastContentChunk](https://github.com/andrestubbe/FastContentChunk) — High-performance native SIMD tokenizer and multi-mode strategy chunker
- [FastAIRag](https://github.com/andrestubbe/FastAIRag) — Retrieval-Augmented Generation pipeline client
- [FastCore](https://github.com/andrestubbe/FastCore) — Native JNI loader for FastJava libraries
- [FastAI](https://github.com/andrestubbe/fastai) — Unified lightweight AI model client interface
- [FastAIModel](https://github.com/andrestubbe/FastAIModel) — Embedded GGUF and ONNX runtimes for local feature embeddings
- [FastAIBot](https://github.com/andrestubbe/FastAIBot) — Autonomous conversational AI bot engine
- [FastAIAgent](https://github.com/andrestubbe/FastAIAgent) — Autonomous agentic workflow execution framework

---

Part of the FastJava Ecosystem — Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀📋
