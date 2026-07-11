# FastAIVectorDB 0.1.0 — Ultrafast Native Vector Database for Java

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe)

---

**💡 Raw JNI performance with pure Java fallbacks — Zero allocation vector database built for high-throughput JVM environments.**

FastAIVectorDB is a **minimalist, hyper-fast JNI vector store** tailored for developers who need maximum similarity lookup performance without running heavy Python processes, Docker instances, or bloated database setups. It features a compiled C++ core utilizing direct memory layout optimization, with a complete pure-Java execution fallback.

---

## Technical Features

- **🚀 Native Performance** — Highly optimized vector operations written in raw C++ linked via JNI.
- **🛡️ Pure-Java Fallback** — Instant, automatic fallback to a thread-safe `InMemoryVectorStore` if the native DLL is missing.
- **⚡ Zero Memory Overhead** — Direct memory mappings that prevent garbage collector stalls on vector queries.
- **📦 Zero External Dependencies** — Compile and execute without extra third-party libraries.

---

## Quick Start

```java
import fastaivectordb.*;

try (FastVectorDB db = new FastVectorDB()) {
    float[] embedding = new float[]{0.1f, -0.2f, 0.89f};
    
    // 1. Insert vector entries
    db.insert(new VectorEntry(0, embedding, "Document snippet content"));
    
    // 2. Perform k-Nearest Neighbors similarity scan
    List<SearchResult> hits = db.search(new float[]{0.1f, -0.1f, 0.9f}, 5);
    for (SearchResult hit : hits) {
        System.out.println("ID: " + hit.entry().id() + " Score: " + hit.score());
    }
}
```

---

## Installation

### Maven (JitPack)
Add JitPack repository and dependency to your `pom.xml`:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastAIVectorDB</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

---

## Technical Architecture

```
                 +-----------------------+
                 |    FastVectorDB       |
                 +-----------+-----------+
                             |
             JNI Available?  |
                    +--------+--------+
                    |                 |
                   YES                NO
                    |                 |
                    v                 v
         +----------+----------+  +---+-------------------+
         | FastVectorDBNative  |  | InMemoryVectorStore   |
         | (C++ DLL Core)      |  | (Pure Java Fallback)  |
         +---------------------+  +-----------------------+
```

---

**Part of the FastJava Ecosystem** — *Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀📋*
