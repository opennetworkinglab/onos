package org.onlab.graph;

import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple concrete implementation of a directed graph path.
 */
public class DefaultPath<V extends Vertex, E extends Edge<V>> implements Path<V, E> {

    private final V src;
    private final V dst;
    private final List<E> edges;
    private double cost = 0.0;

    /**
     * Creates a new path from the specified list of edges and cost.
     *
     * @param edges list of path edges
     * @param cost  path cost as a unit-less number
     */
    public DefaultPath(List<E> edges, double cost) {
        checkNotNull(edges, "Edges list must not be null");
        checkArgument(!edges.isEmpty(), "There must be at least one edge");
        this.edges = ImmutableList.copyOf(edges);
        this.src = edges.get(0).src();
        this.dst = edges.get(edges.size() - 1).dst();
        this.cost = cost;
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
    public double cost() {
        return cost;
    }

    @Override
    public List<E> edges() {
        return Collections.unmodifiableList(edges);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src)
                .add("dst", dst)
                .add("cost", cost)
                .add("edges", edges)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, edges, cost);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefaultPath) {
            final DefaultPath other = (DefaultPath) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst) &&
                    Objects.equals(this.cost, other.cost) &&
                    Objects.equals(this.edges, other.edges);
        }
        return false;
    }

}
