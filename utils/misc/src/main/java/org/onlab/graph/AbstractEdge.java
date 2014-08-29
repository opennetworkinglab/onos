package org.onlab.graph;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstract graph edge implementation.
 */
public abstract class AbstractEdge<V extends Vertex> implements Edge<V> {

    private final V src;
    private final V dst;

    /**
     * Creates a new edge between the specified source and destination vertexes.
     *
     * @param src source vertex
     * @param dst destination vertex
     */
    public AbstractEdge(V src, V dst) {
        this.src = checkNotNull(src, "Source vertex cannot be null");
        this.dst = checkNotNull(dst, "Destination vertex cannot be null");
    }

    @Override
    public V src() {
        return src;
    }

    @Override
    public V dst() {
        return dst;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AbstractEdge) {
            final AbstractEdge other = (AbstractEdge) obj;
            return Objects.equals(this.src, other.src) && Objects.equals(this.dst, other.dst);
        }
        return false;
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("src", src)
                .add("dst", dst)
                .toString();
    }
}
