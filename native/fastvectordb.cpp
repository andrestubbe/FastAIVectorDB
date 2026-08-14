#include "fastvectordb.h"
#include <windows.h>
#include <vector>
#include <algorithm>
#include <cmath>
#include <fstream>
#include <immintrin.h>

/**
 * @file fastvectordb.cpp
 * @brief Native JNI implementation for FastAIVectorDB with AVX2 SIMD Vector Acceleration
 */

struct Entry {
    int id;
    std::vector<float> vector;
};

struct Index {
    std::vector<Entry> entries;
};

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

// AVX2 SIMD FMA Cosine Similarity Engine
static float cosineSimilarity(const float* a, const float* b, int len) {
    float dot = 0.f, normA = 0.f, normB = 0.f;
    int i = 0;

    #if defined(__AVX2__) || defined(_M_AMD64)
    __m256 vdot = _mm256_setzero_ps();
    __m256 vnormA = _mm256_setzero_ps();
    __m256 vnormB = _mm256_setzero_ps();

    for (; i <= len - 8; i += 8) {
        __m256 va = _mm256_loadu_ps(a + i);
        __m256 vb = _mm256_loadu_ps(b + i);
        vdot   = _mm256_fmadd_ps(va, vb, vdot);
        vnormA = _mm256_fmadd_ps(va, va, vnormA);
        vnormB = _mm256_fmadd_ps(vb, vb, vnormB);
    }

    alignas(32) float buf[8];
    _mm256_store_ps(buf, vdot);
    for (int k = 0; k < 8; k++) dot += buf[k];

    _mm256_store_ps(buf, vnormA);
    for (int k = 0; k < 8; k++) normA += buf[k];

    _mm256_store_ps(buf, vnormB);
    for (int k = 0; k < 8; k++) normB += buf[k];
    #endif

    for (; i < len; i++) {
        dot   += a[i] * b[i];
        normA += a[i] * a[i];
        normB += b[i] * b[i];
    }
    float denom = sqrtf(normA) * sqrtf(normB);
    return denom == 0.f ? 0.f : dot / denom;
}

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
  (JNIEnv* env, jclass, jlong ptr, jfloatArray query, jint k) {
    Index* idx = reinterpret_cast<Index*>(ptr);
    jsize queryLen = env->GetArrayLength(query);

    std::vector<float> qVec(queryLen);
    env->GetFloatArrayRegion(query, 0, queryLen, qVec.data());

    struct Scored {
        int id;
        float score;
    };

    std::vector<Scored> scored;
    scored.reserve(idx->entries.size());

    for (const auto& e : idx->entries) {
        if ((jsize)e.vector.size() == queryLen) {
            float sim = cosineSimilarity(qVec.data(), e.vector.data(), queryLen);
            scored.push_back({e.id, sim});
        }
    }

    std::sort(scored.begin(), scored.end(), [](const Scored& a, const Scored& b) {
        return a.score > b.score;
    });

    int resultCount = (std::min)((int)scored.size(), (int)k);
    jintArray result = env->NewIntArray(resultCount * 2);
    if (resultCount == 0) return result;

    std::vector<jint> rawResult(resultCount * 2);
    for (int i = 0; i < resultCount; i++) {
        rawResult[i * 2]     = scored[i].id;
        float s              = scored[i].score;
        rawResult[i * 2 + 1] = *reinterpret_cast<jint*>(&s);
    }

    env->SetIntArrayRegion(result, 0, resultCount * 2, rawResult.data());
    return result;
}

JNIEXPORT void JNICALL Java_fastaivectordb_FastVectorDBNative_free
  (JNIEnv* env, jclass, jlong ptr) {
    Index* idx = reinterpret_cast<Index*>(ptr);
    delete idx;
}

JNIEXPORT jint JNICALL Java_fastaivectordb_FastVectorDBNative_size
  (JNIEnv* env, jclass, jlong ptr) {
    Index* idx = reinterpret_cast<Index*>(ptr);
    return idx ? (jint)idx->entries.size() : 0;
}

JNIEXPORT void JNICALL Java_fastaivectordb_FastVectorDBNative_clear
  (JNIEnv* env, jclass, jlong ptr) {
    Index* idx = reinterpret_cast<Index*>(ptr);
    if (idx) idx->entries.clear();
}

JNIEXPORT void JNICALL Java_fastaivectordb_FastVectorDBNative_save
  (JNIEnv* env, jclass, jlong ptr, jstring pathStr) {
    Index* idx = reinterpret_cast<Index*>(ptr);
    if (!idx) return;

    const char* path = env->GetStringUTFChars(pathStr, nullptr);
    std::ofstream out(path, std::ios::binary);
    env->ReleaseStringUTFChars(pathStr, path);

    if (!out.is_open()) return;

    uint32_t count = (uint32_t)idx->entries.size();
    out.write(reinterpret_cast<const char*>(&count), sizeof(count));

    for (const auto& e : idx->entries) {
        int id = e.id;
        uint32_t dim = (uint32_t)e.vector.size();
        out.write(reinterpret_cast<const char*>(&id), sizeof(id));
        out.write(reinterpret_cast<const char*>(&dim), sizeof(dim));
        if (dim > 0) {
            out.write(reinterpret_cast<const char*>(e.vector.data()), dim * sizeof(float));
        }
    }
}

JNIEXPORT void JNICALL Java_fastaivectordb_FastVectorDBNative_load
  (JNIEnv* env, jclass, jlong ptr, jstring pathStr) {
    Index* idx = reinterpret_cast<Index*>(ptr);
    if (!idx) return;

    const char* path = env->GetStringUTFChars(pathStr, nullptr);
    std::ifstream in(path, std::ios::binary);
    env->ReleaseStringUTFChars(pathStr, path);

    if (!in.is_open()) return;

    idx->entries.clear();
    uint32_t count = 0;
    if (!in.read(reinterpret_cast<char*>(&count), sizeof(count))) return;

    for (uint32_t i = 0; i < count; i++) {
        Entry e;
        uint32_t dim = 0;
        if (!in.read(reinterpret_cast<char*>(&e.id), sizeof(e.id))) return;
        if (!in.read(reinterpret_cast<char*>(&dim), sizeof(dim))) return;
        e.vector.resize(dim);
        if (dim > 0) {
            if (!in.read(reinterpret_cast<char*>(e.vector.data()), dim * sizeof(float))) return;
        }
        idx->entries.push_back(std::move(e));
    }
}
