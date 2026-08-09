package fastaivectordb;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.Map;

/**
 * ONNX-based sentence embedder (e.g. TaylorAI/bge-micro-v2, E5-Small).
 * Produces L2-normalized float vectors via mean-pooling over hidden states.
 * Use via {@link Embedder#onnx(Path, Path)}.
 */
public final class OnnxEmbedder implements Embedder, AutoCloseable {

    private final HuggingFaceTokenizer tokenizer;
    private final OrtEnvironment env;
    private final OrtSession session;

    public OnnxEmbedder(Path modelPath, Path tokenizerPath) {
        try {
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath);
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);
            this.session = env.createSession(modelPath.toString(), opts);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load ONNX embedder from: " + modelPath, e);
        }
    }

    @Override
    public float[] embed(String text) {
        try {
            Encoding enc = tokenizer.encode(text);
            long[] inputIds      = enc.getIds();
            long[] attentionMask = enc.getAttentionMask();
            long[] tokenTypeIds  = enc.getTypeIds();
            long[] shape = {1, inputIds.length};

            try (OnnxTensor tIds  = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds),      shape);
                 OnnxTensor tMask = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask), shape);
                 OnnxTensor tType = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenTypeIds),  shape);
                 OrtSession.Result result = session.run(Map.of(
                         "input_ids",      tIds,
                         "attention_mask", tMask,
                         "token_type_ids", tType))) {

                float[][][] hidden = (float[][][]) result.get(0).getValue();
                return normalize(meanPool(hidden[0], attentionMask));
            }
        } catch (OrtException e) {
            throw new RuntimeException("Embedding inference failed", e);
        }
    }

    private float[] meanPool(float[][] hidden, long[] mask) {
        int dim = hidden[0].length;
        float[] pooled = new float[dim];
        int count = 0;
        for (int t = 0; t < hidden.length; t++) {
            if (mask[t] == 1) {
                for (int d = 0; d < dim; d++) pooled[d] += hidden[t][d];
                count++;
            }
        }
        if (count > 0) for (int d = 0; d < dim; d++) pooled[d] /= count;
        return pooled;
    }

    private float[] normalize(float[] v) {
        float norm = 0f;
        for (float x : v) norm += x * x;
        norm = (float) Math.sqrt(norm);
        if (norm > 1e-9f) for (int i = 0; i < v.length; i++) v[i] /= norm;
        return v;
    }

    @Override
    public void close() {
        try {
            if (session  != null) session.close();
            if (env      != null) env.close();
            if (tokenizer != null) tokenizer.close();
        } catch (OrtException e) {
            System.err.println("OnnxEmbedder close error: " + e.getMessage());
        }
    }
}
