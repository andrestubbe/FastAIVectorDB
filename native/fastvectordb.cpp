#include "fastvectordb.h"
#include <windows.h>
#include <vector>
#include <algorithm>
#include <cmath>

/**
 * @file fastvectordb.cpp
 * @brief Native JNI implementation for FastAIVectorDB
 *
 * Phase 1: Linear scan with cosine similarity.
 * Phase 2 (planned): AVX2/AVX512 SIMD + HNSW index.
 *
 * search() returns a flat jintArray with interleaved pairs:
 *   [id0, Float.floatToIntBits(score0), id1, Float.floatToIntBits(score1), ...]
 */

// ============================================================================
// Internal Data Structures
// ============================================================================

struct Entry {
    int id;
    std::vector<float> vector;
};

struct Index {
    std::vector<Entry> entries;
};

// ============================================================================
// DLL Entry Point
// ============================================================================

BOOL APIENTRY DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
            DisableThreadLibraryCalls(hModule);
            break;
        case DLL_PROCESS_DETACH:
            break;
    }
    return TRUE;
}

// ============================================================================
// Internal Helpers
// ============================================================================

static float cosineSimilarity(const float* a, const float* b, int len) {
    float dot = 0.f, normA = 0.f, normB = 0.f;
    for (int i = 0; i < len; i++) {
        dot   += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    float denom = sqrtf(normA) * sqrtf(normB);
    return denom == 0.f ? 0.f : dot / denom;
}

// ============================================================================
// JNI Implementations
// ============================================================================

JNIEXPORT jlong JNICALL Java_fastaivectordb_FastVectorDBNative_create
  (JNIEnv* env, jclass) {
    return reinterpret_cast<jlong>(new Index());
}

JNIEXPORT void JNICALL Java_fastaivectordb_FastVectorDBNative_insert
  (JNIEnv* env, jclass, jlong ptr, jint id, jfloatArray arr) {
    Index* idx = reinterpret_cast<Index*>(ptr);
    jsize len = env->GetArrayLength(arr);
    Entry e;
    e.id = id;
    e.vector.resize(len);
    env->GetFloatArrayRegion(arr, 0, len, e.vector.data());
    idx->entries.push_back(std::move(e));
}

JNIEXPORT jintArray JNICALL Java_fastaivectordb_FastVectorDBNative_search
  (JNIEnv* env, jclass, jlong ptr, jfloatArray arr, jint k) {
    Index* idx = reinterpret_cast<Index*>(ptr);

    jsize qlen = env->GetArrayLength(arr);
    std::vector<float> query(qlen);
    env->GetFloatArrayRegion(arr, 0, qlen, query.data());

    // Score all entries
    std::vector<std::pair<float, int>> scores;
    scores.reserve(idx->entries.size());
    for (const auto& e : idx->entries) {
        int len = (int)std::min((size_t)qlen, e.vector.size());
        float score = cosineSimilarity(query.data(), e.vector.data(), len);
        scores.emplace_back(score, e.id);
    }

    // Partial sort: top-k descending
    std::sort(scores.begin(), scores.end(),
              [](const auto& a, const auto& b) { return a.first > b.first; });

    int actual = (int)std::min((size_t)k, scores.size());

    // Return interleaved [id, Float.floatToIntBits(score), ...]
    jintArray result = env->NewIntArray(actual * 2);
    std::vector<jint> raw(actual * 2);
    for (int i = 0; i < actual; i++) {
        raw[i * 2]     = scores[i].second;                              // id
        raw[i * 2 + 1] = *reinterpret_cast<const jint*>(&scores[i].first); // score bits
    }
    env->SetIntArrayRegion(result, 0, actual * 2, raw.data());
    return result;
}

JNIEXPORT jint JNICALL Java_fastaivectordb_FastVectorDBNative_size
  (JNIEnv* env, jclass, jlong ptr) {
    return (jint)reinterpret_cast<Index*>(ptr)->entries.size();
}

JNIEXPORT void JNICALL Java_fastaivectordb_FastVectorDBNative_clear
  (JNIEnv* env, jclass, jlong ptr) {
    reinterpret_cast<Index*>(ptr)->entries.clear();
}

JNIEXPORT void JNICALL Java_fastaivectordb_FastVectorDBNative_free
  (JNIEnv* env, jclass, jlong ptr) {
    delete reinterpret_cast<Index*>(ptr);
}
