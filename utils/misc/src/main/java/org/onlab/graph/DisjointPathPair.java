/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.onlab.graph;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Pair of disjoint paths.
 *
 * @param <V> type of vertex
 * @param <E> type of edge
 */
public class DisjointPathPair<V extends Vertex, E extends Edge<V>> implements Path<V, E> {

    private final Path<V, E> primary, secondary;

    /**
     * Creates a disjoint path pair from two paths.
     *
     * @param primary   primary path
     * @param secondary secondary path
     */
    public DisjointPathPair(Path<V, E> primary, Path<V, E> secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    @Override
    public V src() {
        return primary.src();
    }

    @Override
    public V dst() {
        return primary.dst();
    }

    /**
     * Returns the primary path.
     *
     * @return primary path
     */
    public Path<V, E> primary() {
        return primary;
    }

    /**
     * Returns the secondary path.
     *
     * @return secondary path, or null if there is no secondary path available.
     */
    public Path<V, E> secondary() {
        return secondary;
    }

    @Override
    public Weight cost() {
        return hasBackup() ? primary.cost().merge(secondary.cost()) : primary.cost();
    }

    @Override
    public List<E> edges() {
        return primary.edges();
    }

    /**
     * Checks if this path pair contains a backup/secondary path.
     *
     * @return boolean representing whether it has backup
     */
    public boolean hasBackup() {
        return secondary != null;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src())
                .add("dst", dst())
                .add("cost", cost())
                .add("edges", edges())
                .toString();
    }

    @Override
    public int hashCode() {
        // Note: DisjointPathPair with primary and secondary swapped
        // must result in same hashCode
        return hasBackup() ? primary.hashCode() + secondary.hashCode() :
                Objects.hash(primary);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DisjointPathPair) {
            final DisjointPathPair other = (DisjointPathPair) obj;
            return Objects.equals(this.src(), other.src()) &&
                    Objects.equals(this.dst(), other.dst()) &&
                    (Objects.equals(this.primary, other.primary) &&
                            Objects.equals(this.secondary, other.secondary)) ||
                    (Objects.equals(this.primary, other.secondary) &&
                            Objects.equals(this.secondary, other.primary));
        }
        return false;
    }

    /**
     * Returns number of paths inside this path pair object.
     *
     * @return number of paths
     */
    public int size() {
        return hasBackup() ? 2 : 1;
    }
}
