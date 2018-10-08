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
 */
package org.onosproject.openstacknode.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;


/**
 * Implementation of dpdk interface.
 */
public final class DefaultDpdkInterface implements DpdkInterface {
    private final String deviceName;
    private final String intf;
    private final String pciAddress;
    private final Type type;
    private final Long mtu;
    private static final String NOT_NULL_MSG = "% cannot be null";

    private DefaultDpdkInterface(String deviceName,
                                 String intf,
                                 String pciAddress,
                                 Type type,
                                 Long mtu) {
        this.deviceName = deviceName;
        this.intf = intf;
        this.pciAddress = pciAddress;
        this.type = type;
        this.mtu = mtu;
    }

    /**
     * Returns the name of the device where the dpdk interface is.
     *
     * @return device name
     */
    @Override
    public String deviceName() {
        return deviceName;
    }

    /**
     * Returns the name of the dpdk interface.
     *
     * @return dpdk interface name
     */
    @Override
    public String intf() {
        return intf;
    }

    /**
     * Returns the dpdk device arguments of this dpdk port.
     * ex) "0000:85:00.1"
     *
     * @return pci address
     */
    @Override
    public String pciAddress() {
        return pciAddress;
    }

    /**
     * Returns the dpdk interface type.
     *
     * @return type
     */
    @Override
    public Type type() {
        return type;
    }

    /**
     * Returns the mtu size.
     *
     * @return mtu
     */
    @Override
    public Long mtu() {
        return mtu;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("deviceName", deviceName)
                .add("intf", intf)
                .add("pciAddress", pciAddress)
                .add("type", type)
                .add("mtu", mtu)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceName,
                intf,
                pciAddress,
                type,
                mtu);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultDpdkInterface)) {
            return false;
        }
        DefaultDpdkInterface that = (DefaultDpdkInterface) o;
        return Objects.equals(deviceName, that.deviceName) &&
                Objects.equals(intf, that.intf) &&
                Objects.equals(pciAddress, that.pciAddress) &&
                type == that.type &&
                Objects.equals(mtu, that.mtu);
    }

    /**
     * Returns new builder instance.
     *
     * @return dpdk interface builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of dpdk interface instance.
     */
    public static final class Builder implements DpdkInterface.Builder {
        private String deviceName;
        private String intf;
        private String pciAddress;
        private Type type;
        private Long mtu = DpdkInterface.DEFAULT_MTU_SIZE;

        private Builder() {
        }

        @Override
        public DpdkInterface build() {
            checkArgument(deviceName != null, NOT_NULL_MSG, "deviceName");
            checkArgument(intf != null, NOT_NULL_MSG, "intf");
            checkArgument(pciAddress != null, NOT_NULL_MSG, "pciAddress");
            checkArgument(type != null, NOT_NULL_MSG, "type");

            return new DefaultDpdkInterface(deviceName,
                    intf,
                    pciAddress,
                    type,
                    mtu);
        }

        @Override
        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        @Override
        public Builder intf(String name) {
            this.intf = name;
            return this;
        }

        @Override
        public Builder pciAddress(String pciAddress) {
            this.pciAddress = pciAddress;
            return this;
        }

        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder mtu(Long mtu) {
            this.mtu = mtu;
            return this;
        }
    }
}
