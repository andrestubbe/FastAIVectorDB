package fastaivectordb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ultrafast vector store backed by a native C++ engine (JNI).
 * Automatically falls back to {@link InMemoryVectorStore} if the native
 * library cannot be loaded (e.g., during tests or when the DLL is absent).
 */
public final class FastVectorDB implements VectorStore {

    private static final boolean NATIVE_AVAILABLE;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("fastvectordb");
            loaded = true;
        } catch (UnsatisfiedLinkError e) {
            String userDir = System.getProperty("user.dir");
            String[] dirs = {
                userDir + "\\lib\\",
                userDir + "\\build\\",
                userDir + "\\"
            };
            for (String dir : dirs) {
                try {
                    System.load(dir + "fastvectordb.dll");
                    loaded = true;
                    break;
                } catch (UnsatisfiedLinkError ignored) {}
            }
            if (!loaded) {
                System.err.println("FastVectorDB: native lib not found — using Java fallback.");
            }
        }
        NATIVE_AVAILABLE = loaded;
    }

    private final long ptr;
    private final InMemoryVectorStore fallback;
    private final Map<Integer, VectorEntry> textMap = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public FastVectorDB() {
        if (NATIVE_AVAILABLE) {
            this.ptr      = FastVectorDBNative.create();
            this.fallback = null;
        } else {
            this.ptr      = 0L;
            this.fallback = new InMemoryVectorStore();
        }
    }

    @Override
    public void insert(VectorEntry entry) {
        checkOpen();
        if (fallback != null) {
            fallback.insert(entry);
            return;
        }
        FastVectorDBNative.insert(ptr, entry.id(), entry.vector());
        textMap.put(entry.id(), entry);
    }

    @Override
    public List<SearchResult> search(float[] query, int k) {
        checkOpen();
        if (fallback != null) {
            return fallback.search(query, k);
        }
        int[] raw = FastVectorDBNative.search(ptr, query, k);
        List<SearchResult> results = new ArrayList<>(k);
        for (int i = 0; i < raw.length - 1; i += 2) {
            int   id    = raw[i];
            float score = Float.intBitsToFloat(raw[i + 1]);
            VectorEntry entry = textMap.getOrDefault(id, new VectorEntry(id, new float[0], ""));
            results.add(new SearchResult(entry, score));
        }
        return results;
    }

    @Override
    public int size() {
        checkOpen();
        return fallback != null ? fallback.size() : FastVectorDBNative.size(ptr);
    }

    @Override
    public void clear() {
        checkOpen();
        if (fallback != null) {
            fallback.clear();
        } else {
            FastVectorDBNative.clear(ptr);
            textMap.clear();
        }
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;
            if (fallback == null) {
                FastVectorDBNative.free(ptr);
            }
        }
    }

    public static boolean isNativeAvailable() {
        return NATIVE_AVAILABLE;
    }

    private void checkOpen() {
        if (closed) throw new IllegalStateException("FastVectorDB is closed");
    }
}
