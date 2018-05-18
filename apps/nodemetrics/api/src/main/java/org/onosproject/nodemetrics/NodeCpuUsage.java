/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.nodemetrics;

import com.google.common.base.MoreObjects;
import org.onosproject.cluster.NodeId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents CPU usage info of Cluster controllers.
 */
public final class NodeCpuUsage {
    private final NodeId node;
    private final double usage;

    private NodeCpuUsage(final NodeId node, final Double usage) {
        this.node = node;
        this.usage = usage;
    }

    /**
     * Overall usage of CPU includes combined usage of
     * (user,sys,nice,idle,wait,irq,softIrq and stolen) etc.
     * @return usage is overall usage of CPU for the Specific Node.
     */
    public double usage() {
        return usage;
    }

    @Override
    public String toString() {

        return MoreObjects.toStringHelper(this)
                .add("node", this.node)
                .add("usage", String.format("%.2f%s", this.usage, "%"))
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, usage);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NodeCpuUsage other = (NodeCpuUsage) obj;
        return Objects.equals(this.node, other.node)
                && Objects.equals(this.usage, other.usage);
    }

    /**
     * Builder for the DefaultNodeCpu object.
     */
    public static final class Builder {
        /**
         * Builds the DefaultNodeCpu.
         **/
        private NodeId node;
        private Double usage;

        /**
         * Sets the DefaultNodeCpu usage from Library.
         *
         * @param usage of CPU
         * @return self for chaining
         */
        public Builder usage(final Double usage) {
            this.usage = usage;
            return this;
        }

        /**
         * Sets the new DefaultNodeCpu controller node id.
         *
         * @param node the nodeId
         * @return self for chaining
         */
        public Builder withNode(final NodeId node) {
            this.node = node;
            return this;
        }

        public NodeCpuUsage build() {
            checkNotNull(node, "Must specify an node id");
            checkNotNull(usage, "Must specify a usage");
            return new NodeCpuUsage(node, usage);
        }
    }

}
