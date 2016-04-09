/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.topology;

import org.onosproject.net.Link;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of the topology edge backed by a link.
 */
public class DefaultTopologyEdge implements TopologyEdge {

    private final Link link;
    private final TopologyVertex src;
    private final TopologyVertex dst;

    /**
     * Creates a new topology edge.
     *
     * @param src  source vertex
     * @param dst  destination vertex
     * @param link infrastructure link
     */
    public DefaultTopologyEdge(TopologyVertex src, TopologyVertex dst, Link link) {
        this.src = src;
        this.dst = dst;
        this.link = checkNotNull(link);
    }

    @Override
    public Link link() {
        return link;
    }

    @Override
    public TopologyVertex src() {
        return src;
    }

    @Override
    public TopologyVertex dst() {
        return dst;
    }

    @Override
    public int hashCode() {
        return link.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTopologyEdge) {
            final DefaultTopologyEdge other = (DefaultTopologyEdge) obj;
            return Objects.equals(this.link, other.link);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("src", src).add("dst", dst).toString();
    }

}

