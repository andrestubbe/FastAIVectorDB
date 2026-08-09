package fastaivectordb;

import java.nio.file.Path;

/**
 * Strategy interface for text-to-vector embedding.
 * Implement this to plug in any embedding model into FastAIVectorDB.
 *
 * <p>Factory methods:</p>
 * <pre>
 *   Embedder bge    = Embedder.onnx(modelPath, tokenizerPath); // ONNX model
 *   Embedder custom = text -> myModel.embed(text);             // Lambda
 * </pre>
 */
@FunctionalInterface
public interface Embedder {

    /**
     * Embed a single text into a float vector.
     * The returned vector should be L2-normalized for cosine similarity.
     */
    float[] embed(String text);

    /**
     * Creates an ONNX-based embedder (e.g. BGE-Micro-v2, E5-Small).
     * The model and tokenizer are loaded once and reused for all calls.
     *
     * @param modelPath     path to the .onnx model file
     * @param tokenizerPath path to the tokenizer.json file
     */
    static Embedder onnx(Path modelPath, Path tokenizerPath) {
        return new OnnxEmbedder(modelPath, tokenizerPath);
    }

    /**
     * Convenience overload using string paths.
     */
    static Embedder onnx(String modelPath, String tokenizerPath) {
        return new OnnxEmbedder(Path.of(modelPath), Path.of(tokenizerPath));
    }
}
