/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.provider.ProviderId;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a network path.
 */
public class DefaultPath extends DefaultLink implements Path {

    private final List<Link> links;
    private final Weight cost;

    /**
     * Creates a path from the specified source and destination using the
     * supplied list of links.
     *
     * @param providerId provider identity
     * @param links      contiguous links that comprise the path
     * @param cost       unit-less path cost
     * @param annotations optional key/value annotations
     */
    public DefaultPath(ProviderId providerId, List<Link> links, Weight cost,
                       Annotations... annotations) {
        super(providerId, source(links), destination(links), Type.INDIRECT, State.ACTIVE, annotations);
        this.links = ImmutableList.copyOf(links);
        this.cost = cost;
    }

    @Override
    public List<Link> links() {
        return links;
    }

    @Override
    public double cost() {
        if (cost instanceof ScalarWeight) {
            return ((ScalarWeight) cost).value();
        }
        return 0;
    }

    @Override
    public Weight weight() {
        return cost;
    }

    // Returns the source of the first link.
    private static ConnectPoint source(List<Link> links) {
        checkNotNull(links, "List of path links cannot be null");
        checkArgument(!links.isEmpty(), "List of path links cannot be empty");
        return links.get(0).src();
    }

    // Returns the destination of the last link.
    private static ConnectPoint destination(List<Link> links) {
        checkNotNull(links, "List of path links cannot be null");
        checkArgument(!links.isEmpty(), "List of path links cannot be empty");
        return links.get(links.size() - 1).dst();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("src", src())
                .add("dst", dst())
                .add("type", type())
                .add("state", state())
                .add("expected", isExpected())
                .add("links", links)
                .add("cost", cost)
                .toString();
    }

    @Override
    public int hashCode() {
        return links.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPath) {
            final DefaultPath other = (DefaultPath) obj;
            return Objects.equals(this.links, other.links);
        }
        return false;
    }
}
