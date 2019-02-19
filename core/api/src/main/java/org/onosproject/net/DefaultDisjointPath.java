/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net;

import org.onlab.graph.Weight;
import org.onosproject.net.provider.ProviderId;

import java.util.List;
import java.util.Objects;

/**
 * Default implementation of a network disjoint path pair.
 */
public class DefaultDisjointPath extends DefaultPath implements DisjointPath {

    private final DefaultPath path1;
    private final DefaultPath path2;

    /**
     * Creates a disjoint path pair from two default paths.
     *
     * @param providerId provider identity
     * @param path1      primary path
     * @param path2      backup path
     */
    public DefaultDisjointPath(ProviderId providerId, DefaultPath path1, DefaultPath path2) {
        // Note: cost passed to super will never be used
        super(providerId, path1.links(), path1.weight());
        this.path1 = path1;
        this.path2 = path2;
    }

    /**
     * Creates a disjoint path pair from single default paths.
     *
     * @param providerId provider identity
     * @param path1      primary path
     */
    public DefaultDisjointPath(ProviderId providerId, DefaultPath path1) {
        this(providerId, path1, null);
    }

    @Override
    public List<Link> links() {
        return path1.links();
    }

    @Override
    public double cost() {
        return path1.cost();
    }

    @Override
    public Weight weight() {
        return path1.weight();
    }

    @Override
    public Path primary() {
        return path1;
    }

    @Override
    public Path backup() {
        return path2;
    }

    @Override
    public int hashCode() {
        // Note: DisjointPath with primary and secondary swapped
        // must result in same hashCode
        return Objects.hash(Objects.hashCode(path1) + Objects.hashCode(path2), src(), dst());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultDisjointPath) {
            final DefaultDisjointPath other = (DefaultDisjointPath) obj;
            return (Objects.equals(this.path1, other.path1) && Objects.equals(this.path2, other.path2)) ||
                   (Objects.equals(this.path1, other.path2) && Objects.equals(this.path2, other.path1));
        }
        return false;
    }
}
