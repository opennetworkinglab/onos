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
package org.onosproject.cluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Cluster metadata.
 * <p>
 * Metadata specifies the attributes that define a ONOS cluster and comprises the collection
 * of {@link org.onosproject.cluster.ControllerNode nodes} and the collection of data
 * {@link org.onosproject.cluster.Partition partitions}.
 */
public final class ClusterMetadata {

    private String name;
    private Set<ControllerNode> nodes;
    private Set<Partition> partitions;

    /**
     * Returns a new cluster metadata builder.
     * @return The cluster metadata builder.
     */
    public static Builder builder() {
        return new Builder();
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
     * Returns the collection of data {@link org.onosproject.cluster.Partition partitions} that make up the cluster.
     * @return collection of partitions.
     */
    public Collection<Partition> getPartitions() {
        return this.partitions;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(ClusterMetadata.class)
                .add("name", name)
                .add("nodes", nodes)
                .add("partitions", partitions)
                .toString();
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] {name, nodes, partitions});
    }

    /*
     * Provide a deep quality check of the meta data (non-Javadoc)
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

    /**
     * Builder for a {@link ClusterMetadata} instance.
     */
    public static class Builder {

        private final ClusterMetadata metadata;

        public Builder() {
            metadata = new ClusterMetadata();
        }

        /**
         * Sets the cluster name, returning the cluster metadata builder for method chaining.
         * @param name cluster name
         * @return this cluster metadata builder
         */
        public Builder withName(String name) {
            metadata.name = checkNotNull(name);
            return this;
        }

        /**
         * Sets the collection of cluster nodes, returning the cluster metadata builder for method chaining.
         * @param controllerNodes collection of cluster nodes
         * @return this cluster metadata builder
         */
        public Builder withControllerNodes(Collection<ControllerNode> controllerNodes) {
            metadata.nodes = ImmutableSet.copyOf(checkNotNull(controllerNodes));
            return this;
        }

        /**
         * Sets the collection of data partitions, returning the cluster metadata builder for method chaining.
         * @param partitions collection of partitions
         * @return this cluster metadata builder
         */
        public Builder withPartitions(Collection<Partition> partitions) {
            metadata.partitions = ImmutableSet.copyOf(checkNotNull(partitions));
            return this;
        }

        /**
         * Builds the cluster metadata.
         * @return cluster metadata
         * @throws com.google.common.base.VerifyException VerifyException if the metadata is misconfigured
         */
        public ClusterMetadata build() {
            verifyMetadata();
            return metadata;
        }

        /**
         * Validates the constructed metadata for semantic correctness.
         * @throws VerifyException if the metadata is misconfigured.
         */
        private void verifyMetadata() {
            verifyNotNull(metadata.getName(), "Cluster name must be specified");
            verifyNotNull(metadata.getNodes(), "Cluster nodes must be specified");
            verifyNotNull(metadata.getPartitions(), "Cluster partitions must be specified");
            verify(!metadata.getNodes().isEmpty(), "Cluster nodes must not be empty");
            verify(!metadata.getPartitions().isEmpty(), "Cluster nodes must not be empty");

            // verify that partitions are constituted from valid cluster nodes.
            boolean validPartitions = Collections2.transform(metadata.getNodes(), ControllerNode::id)
                    .containsAll(metadata.getPartitions()
                            .stream()
                            .flatMap(r -> r.getMembers().stream())
                            .collect(Collectors.toSet()));
            verify(validPartitions, "Partition locations must be valid cluster nodes");
        }
    }
}
