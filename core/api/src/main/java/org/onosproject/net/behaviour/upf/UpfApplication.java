/*
 * Copyright 2022-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;
import com.google.common.collect.Range;
import org.onlab.packet.Ip4Prefix;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A structure representing the application filtering for the UPF-programmable device.
 */
@Beta
public final class UpfApplication implements UpfEntity {
    // Match Keys
    private final Ip4Prefix ipPrefix;
    private final Range<Short> l4PortRange;
    private final Byte ipProto;
    // TODO: move to SliceId object when slice APIs will be promoted to ONOS core.
    private final int sliceId;
    // Action parameter
    private final byte appId;

    private final int priority;

    private UpfApplication(Ip4Prefix ipPrefix, Range<Short> l4PortRange,
                           Byte ipProto, int sliceId, byte appId, int priority) {
        this.ipPrefix = ipPrefix;
        this.l4PortRange = l4PortRange;
        this.ipProto = ipProto;
        this.sliceId = sliceId;
        this.appId = appId;
        this.priority = priority;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }

        UpfApplication that = (UpfApplication) object;

        return Objects.equals(this.ipPrefix, that.ipPrefix) &&
                Objects.equals(this.l4PortRange, that.l4PortRange) &&
                Objects.equals(this.ipProto, that.ipProto) &&
                this.sliceId == that.sliceId &&
                this.appId == that.appId &&
                this.priority == that.priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipPrefix, l4PortRange, ipProto, sliceId, appId, priority);
    }

    @Override
    public String toString() {
        return "UpfApplication(priority=" + this.priority + ", " + matchString() + " -> " + actionString() + ")";
    }

    private String matchString() {
        StringBuilder matchStrBuilder = new StringBuilder("Match(");
        if (this.ipPrefix != null) {
            matchStrBuilder.append("ip_prefix=")
                    .append(this.ipPrefix)
                    .append(", ");
        }
        if (this.l4PortRange != null) {
            matchStrBuilder.append("l4_port_range=")
                    .append(l4PortRange)
                    .append(", ");
        }
        if (this.ipProto != null) {
            matchStrBuilder.append("ip_proto=")
                    .append(this.ipProto)
                    .append(", ");
        }
        matchStrBuilder.append("slice_id=")
                .append(this.sliceId)
                .append(")");
        return matchStrBuilder.toString();
    }

    private String actionString() {
        return "Action(app_id=" + this.appId + ")";
    }

    /**
     * Gets the IPv4 prefix of this UPF application rule.
     *
     * @return The IPv4 prefix, Empty if none.
     */
    public Optional<Ip4Prefix> ip4Prefix() {
        return Optional.ofNullable(ipPrefix);
    }

    /**
     * Gets the L4 port range of this application filtering rule.
     *
     * @return A bounded range of L4 port
     */
    public Optional<Range<Short>> l4PortRange() {
        return Optional.ofNullable(l4PortRange);
    }

    /**
     * Gets the IP protocol field value of this UPF application rule.
     *
     * @return IP protocol field, Empty if none
     */
    public Optional<Byte> ipProto() {
        return Optional.ofNullable(ipProto);
    }

    /**
     * Gets the slice ID of this UPF application rule.
     *
     * @return Slice ID
     */
    public int sliceId() {
        return this.sliceId;
    }

    /**
     * Get the application ID of this UPF application rule.
     *
     * @return Application ID
     */
    public byte appId() {
        return appId;
    }

    /**
     * Get the priority of this UPF application rule.
     *
     * @return Priority
     */
    public int priority() {
        return priority;
    }

    @Override
    public UpfEntityType type() {
        return UpfEntityType.APPLICATION;
    }

    /**
     * Builder of UpfApplication object.
     */
    public static class Builder {
        // Match Keys
        private Ip4Prefix ipPrefix = null;
        private Range<Short> l4PortRange = null;
        private Byte ipProto = null;
        private Integer sliceId = null;
        // Action parameters
        private Byte appId = null;

        private Integer priority = null;

        public Builder() {

        }

        /**
         * Set the IP prefix of the UPF application rule.
         *
         * @param ipPrefix IPv4 prefix
         * @return This builder object
         */
        public Builder withIp4Prefix(Ip4Prefix ipPrefix) {
            this.ipPrefix = ipPrefix;
            return this;
        }

        /**
         * Set the L4 port range of the UPF application rule.
         *
         * @param l4PortRange bounded range of L4 port
         * @return This builder object
         */
        public Builder withL4PortRange(Range<Short> l4PortRange) {
            checkArgument(l4PortRange.hasLowerBound() && l4PortRange.hasUpperBound(),
                          "Range must be provided with bounds");
            this.l4PortRange = l4PortRange;
            return this;
        }

        /**
         * Set the IP protocol field value of the UPF application rule.
         *
         * @param ipProto IP protocol field
         * @return This builder object
         */
        public Builder withIpProto(byte ipProto) {
            this.ipProto = ipProto;
            return this;
        }

        /**
         * Set the slice ID of the UPF application rule.
         *
         * @param sliceId the slice ID
         * @return This builder object
         */
        public Builder withSliceId(int sliceId) {
            this.sliceId = sliceId;
            return this;
        }

        /**
         * Set the application ID of the UPF application rule.
         *
         * @param appId Application ID
         * @return This builder object
         */
        public Builder withAppId(byte appId) {
            this.appId = appId;
            return this;
        }

        /**
         * Set the priority of the UPF application rule.
         *
         * @param priority Priority
         * @return This builder object
         */
        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public UpfApplication build() {
            checkNotNull(sliceId, "Slice ID must be provided");
            checkNotNull(appId, "Application ID must be provided");
            checkNotNull(priority, "Priority must be provided");
            return new UpfApplication(ipPrefix, l4PortRange, ipProto, sliceId, appId, priority);
        }
    }
}
