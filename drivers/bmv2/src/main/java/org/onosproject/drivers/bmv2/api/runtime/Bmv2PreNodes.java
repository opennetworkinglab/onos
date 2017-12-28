/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */

package org.onosproject.drivers.bmv2.api.runtime;


import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable collection of BMv2 PRE group nodes.
 */
public final class Bmv2PreNodes {
    private final ImmutableSet<Bmv2PreNode> nodes;

    /**
     * Creates a immutable list of group nodes.
     *
     * @param nodes list of group nodes
     */
    public Bmv2PreNodes(ImmutableSet<Bmv2PreNode> nodes) {
        this.nodes = checkNotNull(nodes);
    }

    public ImmutableSet<Bmv2PreNode> nodes() {
        return nodes;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nodes);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bmv2PreNodes) {
            return (this.nodes.containsAll(((Bmv2PreNodes) obj).nodes) &&
                    ((Bmv2PreNodes) obj).nodes.containsAll(this.nodes));
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("nodes", nodes)
                .toString();
    }
}
