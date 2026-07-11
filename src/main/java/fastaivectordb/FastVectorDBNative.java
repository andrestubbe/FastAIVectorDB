package fastaivectordb;

/**
 * Raw JNI bindings for the native fastvectordb library.
 * Not part of the public API — use {@link FastVectorDB} instead.
 *
 * search() returns a flat int[] where pairs [id, Float.floatToIntBits(score)]
 * are interleaved: [id0, scoreBits0, id1, scoreBits1, ...].
 */
final class FastVectorDBNative {

    private FastVectorDBNative() {}

    static native long   create();
    static native void   insert(long ptr, int id, float[] vector);
    static native int[]  search(long ptr, float[] query, int k);
    static native int    size(long ptr);
    static native void   clear(long ptr);
    static native void   free(long ptr);
}
