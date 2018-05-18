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
 * Represents Memory usage of Cluster controllers.
 */
public final class NodeMemoryUsage {

    private final NodeId node;

    private final Units units;

    private final long free;

    private final long used;

    private final long total;

    private final double usage;

    private static final double PERCENTAGE_MULTIPLIER = 100.0;

    private NodeMemoryUsage(final NodeId node, final Units units,
                      final Long free, final Long used, final Long total,
                      final Double usage) {
        this.node = node;
        this.units = units;
        this.free = free;
        this.used = used;
        this.total = total;
        this.usage = usage;
    }

    /**
     * @return free memory available for the specific nodeId.
     */
    public long free() {
        return free;
    }

    /**
     * @return used memory for the specific nodeId..
     */
    public long used() {
        return used;
    }

    /**
     * @return total memory for the specific nodeId..
     */
    public long total() {
        return total;
    }

    /**
     * @return memory in Kbs / Mbs etc.
     */
    public Units units() {
        return units;
    }

    /**
     * Percentage of Memory usage is calculated from Used, Available and Total Memory.
     * @return usage overall usage of memory in percentage for the specific node.
     */
    public double usage() {
        return usage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, free, used, total, units, usage);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("node", this.node)
                .add("free", this.free)
                .add("used", this.used)
                .add("total", this.total)
                .add("units", this.units.toString())
                .add("usage", this.usage + "%")
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        NodeMemoryUsage other = (NodeMemoryUsage) obj;
        return Objects.equals(this.node, other.node)
                && Objects.equals(this.free, other.free)
                && Objects.equals(this.used, other.used)
                && Objects.equals(this.total, other.total)
                && Objects.equals(this.units, other.units)
                && Objects.equals(this.usage, other.usage);
    }

    /**
     * Builder for the DefaultNodeMemory object.
     */
    public static final class Builder {
        /**
         * Builds the DefaultNodeMemory.
         **/
        private NodeId node;
        private Units unit;
        private Long free;
        private Long used;
        private Long total;

        /**
         * Sets the new DefaultNodeMemory controller nodeId.
         *
         * @param node nodeId
         * @return self for chaining
         */
        public Builder withNode(final NodeId node) {
            this.node = node;
            return this;
        }

        /**
         * Sets the new DefaultNodeMemory Disk size units(bytes, Kbs, Mbs, Gbs).
         *
         * @param unit units
         * @return self for chaining
         */
        public Builder withUnit(final Units unit) {
            this.unit = unit;
            return this;
        }

        /**
         * Sets the new DefaultNodeMemory controller free space.
         *
         * @param free free space
         * @return self for chaining
         */
        public Builder free(final Long free) {
            this.free = free;
            return this;
        }

        /**
         * Sets the new DefaultNodeMemory controller used space.
         *
         * @param used used space
         * @return self for chaining
         */
        public Builder used(final Long used) {
            this.used = used;
            return this;
        }

        /**
         * Sets the new DefaultNodeMemory controller total disk.
         *
         * @param total the total disk space
         * @return self for chaining
         */
        public Builder total(final Long total) {
            this.total = total;
            return this;
        }

        public NodeMemoryUsage build() {
            checkNotNull(node, "Must specify an node id");
            checkNotNull(unit, "Must specify a unit");
            checkNotNull(used, "Must specify a used Diskspace");
            checkNotNull(free, "Must specify a free Diskspace");
            checkNotNull(total, "Must specify a total Diskspace");
            double usage = used * PERCENTAGE_MULTIPLIER / total;
            return new NodeMemoryUsage(node, unit, free, used, total, usage);
        }

    }

}