#ifndef FASTVECTORDB_H
#define FASTVECTORDB_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// Export declarations (Matches fastvectordb.def)
JNIEXPORT jlong  JNICALL Java_fastaivectordb_FastVectorDBNative_create  (JNIEnv*, jclass);
JNIEXPORT void   JNICALL Java_fastaivectordb_FastVectorDBNative_insert  (JNIEnv*, jclass, jlong, jint, jfloatArray);
JNIEXPORT jintArray JNICALL Java_fastaivectordb_FastVectorDBNative_search(JNIEnv*, jclass, jlong, jfloatArray, jint);
JNIEXPORT jint   JNICALL Java_fastaivectordb_FastVectorDBNative_size    (JNIEnv*, jclass, jlong);
JNIEXPORT void   JNICALL Java_fastaivectordb_FastVectorDBNative_clear   (JNIEnv*, jclass, jlong);
JNIEXPORT void   JNICALL Java_fastaivectordb_FastVectorDBNative_free    (JNIEnv*, jclass, jlong);
JNIEXPORT void   JNICALL Java_fastaivectordb_FastVectorDBNative_save    (JNIEnv*, jclass, jlong, jstring);
JNIEXPORT void   JNICALL Java_fastaivectordb_FastVectorDBNative_load    (JNIEnv*, jclass, jlong, jstring);

#ifdef __cplusplus
}
#endif

#endif // FASTVECTORDB_H
