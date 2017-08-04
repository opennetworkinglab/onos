/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.region;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.AbstractAnnotated;
import org.onosproject.net.Annotations;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation of a region.
 */
public final class DefaultRegion extends AbstractAnnotated implements Region {

    private static final int NAME_MAX_LENGTH = 1024;

    private final RegionId id;
    private final String name;
    private final Type type;
    private final List<Set<NodeId>> masters;

    /**
     * Creates a region using the supplied information.
     *
     * @param id      region identifier
     * @param name    friendly name
     * @param type    region type
     * @param annots  annotations
     * @param masters list of sets of cluster node identifiers; in order of mastership
     */
    public DefaultRegion(RegionId id, String name, Type type,
                         Annotations annots, List<Set<NodeId>> masters) {
        super(annots);
        this.id = id;
        this.name = name;
        this.type = type;
        this.masters = masters != null ? ImmutableList.copyOf(masters) : ImmutableList.of();
        if (name != null) {
            checkArgument(name.length() <= NAME_MAX_LENGTH, "name exceeds maximum length " + NAME_MAX_LENGTH);
        }
    }

    @Override
    public RegionId id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public List<Set<NodeId>> masters() {
        return masters;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, type, masters);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultRegion) {
            final DefaultRegion that = (DefaultRegion) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.name, that.name)
                    && Objects.equals(this.type, that.type)
                    && Objects.equals(this.masters, that.masters);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("type", type)
                .add("masters", masters)
                .toString();
    }

}
