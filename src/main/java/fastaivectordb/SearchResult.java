package fastaivectordb;

public record SearchResult(VectorEntry entry, float score) {
}
