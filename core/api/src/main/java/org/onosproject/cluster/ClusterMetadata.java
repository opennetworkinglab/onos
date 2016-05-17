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
package org.onosproject.cluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.onosproject.net.Provided;
import org.onosproject.net.provider.ProviderId;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Charsets.UTF_8;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

/**
 * Cluster metadata.
 * <p>
 * Metadata specifies how a ONOS cluster is constituted and is made up of the collection
 * of {@link org.onosproject.cluster.ControllerNode nodes} and the collection of data
 * {@link org.onosproject.cluster.Partition partitions}.
 */
public final class ClusterMetadata implements Provided {

    private final ProviderId providerId;
    private final String name;
    private final Set<ControllerNode> nodes;
    private final Set<Partition> partitions;

    public static final Funnel<ClusterMetadata> HASH_FUNNEL = new Funnel<ClusterMetadata>() {
        @Override
        public void funnel(ClusterMetadata cm, PrimitiveSink into) {
            into.putString(cm.name, UTF_8);
        }
    };

    @SuppressWarnings("unused")
    private ClusterMetadata() {
        providerId = null;
        name = null;
        nodes = null;
        partitions = null;
    }

    public ClusterMetadata(ProviderId providerId,
            String name,
            Set<ControllerNode> nodes,
            Set<Partition> partitions) {
        this.providerId = checkNotNull(providerId);
        this.name = checkNotNull(name);
        this.nodes = ImmutableSet.copyOf(checkNotNull(nodes));
        // verify that partitions are constituted from valid cluster nodes.
        boolean validPartitions = Collections2.transform(nodes, ControllerNode::id)
                .containsAll(partitions
                        .stream()
                        .flatMap(r -> r.getMembers().stream())
                        .collect(Collectors.toSet()));
        verify(validPartitions, "Partition locations must be valid cluster nodes");
        this.partitions = ImmutableSet.copyOf(checkNotNull(partitions));
    }

    public ClusterMetadata(String name,
            Set<ControllerNode> nodes,
            Set<Partition> partitions) {
        this(new ProviderId("none", "none"), name, nodes, partitions);
    }

    @Override
    public ProviderId providerId() {
        return providerId;
    }

    /**
     * Returns the name of the cluster.
     *
     * @return cluster name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the collection of {@link org.onosproject.cluster.ControllerNode nodes} that make up the cluster.
     * @return cluster nodes
     */
    public Collection<ControllerNode> getNodes() {
        return this.nodes;
    }

    /**
     * Returns the collection of {@link org.onosproject.cluster.Partition partitions} that make
     * up the cluster.
     * @return collection of partitions.
     */
    public Collection<Partition> getPartitions() {
        return this.partitions;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ClusterMetadata.class)
                .add("providerId", providerId)
                .add("name", name)
                .add("nodes", nodes)
                .add("partitions", partitions)
                .toString();
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] {providerId, name, nodes, partitions});
    }

    /*
     * Provide a deep equality check of the cluster metadata (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object object) {

        if (!ClusterMetadata.class.isInstance(object)) {
            return false;
        }
        ClusterMetadata that = (ClusterMetadata) object;

        if (!this.name.equals(that.name) || this.nodes.size() != that.nodes.size()
                || this.partitions.size() != that.partitions.size()) {
            return false;
        }

        return Sets.symmetricDifference(this.nodes, that.nodes).isEmpty()
                && Sets.symmetricDifference(this.partitions, that.partitions).isEmpty();
    }
}
