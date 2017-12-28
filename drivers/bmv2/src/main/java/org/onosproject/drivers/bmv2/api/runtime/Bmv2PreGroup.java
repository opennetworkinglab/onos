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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a multicast group in BMv2 PRE.
 */
public class Bmv2PreGroup implements Bmv2Entity {

    private final Integer groupId;
    private final Bmv2PreNodes nodes;
    //internal device-level identifier used by BMv2
    private Integer nativeGroupHandle;

    public Bmv2PreGroup(Integer groupId, Bmv2PreNodes nodes) {
        this.groupId = checkNotNull(groupId, "groupId argument can not be null");
        this.nodes = checkNotNull(nodes, "nodes argument can not be null");
    }

    /**
     * Returns a new builder of BMv2 PRE groups.
     *
     * @return a BMv2 PRE group builder
     */
    public static Bmv2PreGroupBuilder builder() {
        return new Bmv2PreGroupBuilder();
    }

    public Integer groupId() {
        return groupId;
    }

    public Integer nativeGroupHandle() {
        return nativeGroupHandle;
    }

    public Bmv2PreNodes nodes() {
        return nodes;
    }

    public void setNativeGroupHandle(Integer nativeGroupHandle) {
        this.nativeGroupHandle = nativeGroupHandle;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupId, nodes, nativeGroupHandle);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2PreGroup other = (Bmv2PreGroup) obj;
        return Objects.equal(this.groupId, other.groupId)
                && Objects.equal(this.nodes, other.nodes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("groupId", groupId)
                .add("nativeGroupHandle", nativeGroupHandle)
                .add("nodes", nodes)
                .toString();
    }

    @Override
    public Bmv2EntityType entityType() {
        return Bmv2EntityType.PRE_GROUP;
    }

    /**
     * Builder of BMv2 PRE groups.
     */
    public static final class Bmv2PreGroupBuilder {
        private Integer groupId;
        private Set<Bmv2PreNode> nodes = Sets.newHashSet();

        private Bmv2PreGroupBuilder() {
            //hidden constructor
        }

        /**
         * Sets the identifier of this group.
         *
         * @param groupId identifier of this BMv2 PRE group.
         * @return this
         */
        public Bmv2PreGroupBuilder withGroupId(Integer groupId) {
            this.groupId = groupId;
            return this;
        }

        /**
         * Adds a node to this group.
         *
         * @param node a BMv2 PRE node.
         * @return this
         */
        public Bmv2PreGroupBuilder addNode(Bmv2PreNode node) {
            nodes.add(node);
            return this;
        }

        /**
         * Creates a new BMv2 PRE group.
         *
         * @return a new BMv2 PRE group
         */
        public Bmv2PreGroup build() {
            return new Bmv2PreGroup(groupId, new Bmv2PreNodes(ImmutableSet.copyOf(nodes)));
        }
    }
}
