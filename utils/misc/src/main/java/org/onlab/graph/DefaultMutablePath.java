package org.onlab.graph;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Simple concrete implementation of a directed graph path.
 */
public class DefaultMutablePath<V extends Vertex, E extends Edge<V>> implements MutablePath<V, E> {

    private V src = null;
    private V dst = null;
    private final List<E> edges = new ArrayList<>();
    private double cost = 0.0;

    /**
     * Creates a new empty path.
     */
    public DefaultMutablePath() {
    }

    /**
     * Creates a new path as a copy of another path.
     *
     * @param path path to be copied
     */
    public DefaultMutablePath(Path<V, E> path) {
        checkNotNull(path, "Path cannot be null");
        this.src = path.src();
        this.dst = path.dst();
        this.cost = path.cost();
        edges.addAll(path.edges());
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
        return ImmutableList.copyOf(edges);
    }

    @Override
    public void setCost(double cost) {
        this.cost = cost;
    }

    @Override
    public Path<V, E> toImmutable() {
        return new DefaultPath<>(edges, cost);
    }

    @Override
    public void appendEdge(E edge) {
        checkNotNull(edge, "Edge cannot be null");
        checkArgument(edges.isEmpty() || dst.equals(edge.src()),
                      "Edge source must be the same as the current path destination");
        dst = edge.dst();
        edges.add(edge);
    }

    @Override
    public void insertEdge(E edge) {
        checkNotNull(edge, "Edge cannot be null");
        checkArgument(edges.isEmpty() || src.equals(edge.dst()),
                      "Edge destination must be the same as the current path source");
        src = edge.src();
        edges.add(0, edge);
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
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
        if (obj instanceof DefaultMutablePath) {
            final DefaultMutablePath other = (DefaultMutablePath) obj;
            return super.equals(obj) &&
                    Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst) &&
                    Objects.equals(this.cost, other.cost) &&
                    Objects.equals(this.edges, other.edges);
        }
        return false;
    }
}
