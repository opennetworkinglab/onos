/*
 * Copyright 2021-present Open Networking Foundation
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
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A UPF device interface, such as a N3, or UE IP address pool (N6).
 */
@Beta
public final class UpfInterface implements UpfEntity {
    private final Ip4Prefix prefix;
    private final Type type;
    // TODO: move to SliceId object when slice APIs will be promoted to ONOS core.
    private final int sliceId;

    private UpfInterface(Ip4Prefix prefix, Type type, int sliceId) {
        this.prefix = prefix;
        this.type = type;
        this.sliceId = sliceId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return String.format("UpfInterface(type=%s, prefix=%s, slice_id=%s)", type.toString(), prefix, sliceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UpfInterface that = (UpfInterface) obj;
        return this.type.equals(that.type) &&
                this.prefix.equals(that.prefix) &&
                this.sliceId == that.sliceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, type, sliceId);
    }

    /**
     * Create access-facing UPF Interface (N3) from the given address, which will be treated as a /32 prefix.
     *
     * @param address the address of the new access-facing interface (N3 interface)
     * @param sliceId the slice id of the new interface
     * @return a new UPF interface
     */
    public static UpfInterface createN3From(Ip4Address address, int sliceId) {
        return builder()
                .setAccess()
                .setPrefix(Ip4Prefix.valueOf(address, 32))
                .setSliceId(sliceId)
                .build();
    }

    /**
     * Create a core-facing UPF Interface (N6) from the given IP prefix.
     *
     * @param prefix the prefix of the new core-facing interface (N6 interface)
     * @param sliceId the slice id of the new interface
     * @return a new UPF interface
     */
    public static UpfInterface createUePoolFrom(Ip4Prefix prefix, int sliceId) {
        return builder()
                .setCore()
                .setPrefix(prefix)
                .setSliceId(sliceId)
                .build();
    }

    /**
     * Create a dbuf-receiving UPF interface from the given IP address.
     *
     * @param address the address of the dbuf-receiving interface
     * @param sliceId the slice id of the new interface
     * @return a new UPF interface
     */
    public static UpfInterface createDbufReceiverFrom(Ip4Address address, int sliceId) {
        return builder()
                .setDbufReceiver()
                .setAddress(address)
                .setSliceId(sliceId)
                .build();
    }

    /**
     * Get the IP prefix of this interface.
     *
     * @return the interface prefix
     */
    public Ip4Prefix prefix() {
        return prefix;
    }

    /**
     * Get the slice ID of this interface.
     *
     * @return the slice ID
     */
    public int sliceId() {
        return sliceId;
    }

    /**
     * Check if this UPF interface is for packets traveling from UEs.
     * This will be true for N3 interface table entries.
     *
     * @return true if interface receives from access
     */
    public boolean isAccess() {
        return type == Type.ACCESS;
    }

    /**
     * Check if this UPF interface is for packets traveling towards UEs.
     * This will be true for UE IP address pool table entries.
     *
     * @return true if interface receives from core
     */
    public boolean isCore() {
        return type == Type.CORE;
    }

    /**
     * Check if this UPF interface is for receiving buffered packets as they are released from the dbuf
     * buffering device.
     *
     * @return true if interface receives from dbuf
     */
    public boolean isDbufReceiver() {
        return type == Type.DBUF;
    }

    /**
     * Get the IPv4 prefix of this UPF interface.
     *
     * @return the interface prefix
     */
    public Ip4Prefix getPrefix() {
        return this.prefix;
    }

    @Override
    public UpfEntityType type() {
        return UpfEntityType.INTERFACE;
    }

    public enum Type {
        /**
         * Unknown UPF interface type.
         */
        UNKNOWN,

        /**
         * Interface that receives GTP encapsulated packets.
         * This is the type of the N3 interface.
         */
        ACCESS,

        /**
         * Interface that receives unencapsulated packets from the core of the network.
         * This is the type of UE IP address pool interfaces (N6).
         */
        CORE,

        /**
         * Interface that receives buffered packets as they are drained from a dbuf device.
         */
        DBUF
    }

    public static class Builder {
        private Ip4Prefix prefix;
        private Type type;
        private Integer sliceId;

        public Builder() {
            type = Type.UNKNOWN;
        }

        /**
         * Set the IPv4 prefix of this interface.
         *
         * @param prefix the interface prefix
         * @return this builder object
         */
        public Builder setPrefix(Ip4Prefix prefix) {
            this.prefix = prefix;
            return this;
        }

        /**
         * Set the slice ID of this interface.
         *
         * @param sliceId the slice ID
         * @return this builder object
         */
        public Builder setSliceId(int sliceId) {
            this.sliceId = sliceId;
            return this;
        }

        /**
         * Set the IPv4 prefix of this interface, by turning the given address into a /32 prefix.
         *
         * @param address the interface address that will become a /32 prefix
         * @return this builder object
         */
        public Builder setAddress(Ip4Address address) {
            this.prefix = Ip4Prefix.valueOf(address, 32);
            return this;
        }

        /**
         * Make this an access-facing interface (N3).
         *
         * @return this builder object
         */
        public Builder setAccess() {
            this.type = Type.ACCESS;
            return this;
        }

        /**
         * Make this a core-facing interface (N6).
         *
         * @return this builder object
         */
        public Builder setCore() {
            this.type = Type.CORE;
            return this;
        }

        /**
         * Make this a dbuf-facing interface.
         *
         * @return this builder object
         */
        public Builder setDbufReceiver() {
            this.type = Type.DBUF;
            return this;
        }

        public UpfInterface build() {
            checkNotNull(prefix, "The IPv4 prefix must be provided");
            checkNotNull(sliceId, "Slice ID must be provided");
            return new UpfInterface(prefix, type, sliceId);
        }
    }
}
