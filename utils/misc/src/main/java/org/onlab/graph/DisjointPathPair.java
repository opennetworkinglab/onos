/*
 * Copyright 2015 Open Networking Laboratory
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
import java.util.Set;

import static com.google.common.collect.ImmutableSet.of;
import static com.google.common.base.MoreObjects.toStringHelper;


public class DisjointPathPair<V extends Vertex, E extends Edge<V>> implements Path<V, E> {
    public Path<V, E> path1, path2;
    boolean usingPath1 = true;

    /**
     * Creates a Disjoint Path Pair from two paths.
     *
     * @param p1    first path
     * @param p2    second path
     */
    public DisjointPathPair(Path<V, E> p1, Path<V, E> p2) {
        path1 = p1;
        path2 = p2;
    }

    @Override
    public V src() {
        return path1.src();
    }

    @Override
    public V dst() {
        return path1.dst();
    }

    @Override
    public double cost() {
        if (!hasBackup()) {
            return path1.cost();
        }
        return path1.cost() + path2.cost();
    }

    @Override
    public List<E> edges() {
        if (usingPath1 || !hasBackup()) {
            return path1.edges();
        } else {
            return path2.edges();
        }
    }

    /**
     * Checks if this path pair contains a backup/secondary path.
     *
     * @return boolean representing whether it has backup
     */
    public boolean hasBackup() {
        return path2 != null && path2.edges() != null;
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
        Set<Path<V, E>> paths;
        if (!hasBackup()) {
            paths = of(path1);
        } else {
            paths = of(path1, path2);
        }
        return Objects.hash(paths);
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
                    (Objects.equals(this.path1, other.path1) &&
                            Objects.equals(this.path2, other.path2)) ||
                    (Objects.equals(this.path1, other.path2) &&
                            Objects.equals(this.path2, other.path1));
        }
        return false;
    }

    /**
     * Returns number of paths inside this path pair object.
     *
     * @return number of paths
     */
    public int size() {
        if (hasBackup()) {
            return 2;
        }
        return 1;
    }
}
