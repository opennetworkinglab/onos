/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.xosclient.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of port in a CORD VTN controlled network, it can be for VM
 * or container.
 */
public final class VtnPort {

    private final VtnPortId id;
    private final String name;
    private final VtnServiceId serviceId;
    private final MacAddress mac;
    private final IpAddress ip;
    // TODO remove this when XOS provides vSG information
    private final Map<IpAddress, MacAddress> addressPairs;

   private VtnPort(VtnPortId id,
                   String name,
                   VtnServiceId serviceId,
                   MacAddress mac,
                   IpAddress ip,
                   Map<IpAddress, MacAddress> addressPairs) {
        this.id = id;
        this.name = name;
        this.serviceId = serviceId;
        this.mac = mac;
        this.ip = ip;
        this.addressPairs = addressPairs;
    }

    /**
     * Returns vtn port ID.
     *
     * @return vtn port id
     */
    public VtnPortId id() {
        return id;
    }

    /**
     * Returns vtn port name.
     *
     * @return vtn port name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the ID of the service this port is in.
     *
     * @return vtn service id
     */
    public VtnServiceId serviceId() {
        return serviceId;
    }

    /**
     * Returns MAC address of this port.
     *
     * @return mac address
     */
    public MacAddress mac() {
        return mac;
    }

    /**
     * Returns IP address of this port.
     *
     * @return ip address
     */
    public IpAddress ip() {
        return ip;
    }

    /**
     * Returns address pairs of the nested containers inside.
     *
     * @return map of ip and address
     */
    public Map<IpAddress, MacAddress> addressPairs() {
        return addressPairs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VtnPort)) {
            return false;
        }
        final VtnPort other = (VtnPort) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("name", name)
                .add("serviceId", serviceId)
                .add("mac", mac)
                .add("ip", ip)
                .add("addressPairs", addressPairs)
                .toString();
    }

    /**
     * Returns a new vtn port builder instance.
     *
     * @return new vtn port builder
     */
    public static final Builder builder() {
        return new Builder();
    }

    /**
     * Builder of VTN port entities.
     */
    public static final class Builder {

        private VtnPortId id;
        private String name;
        private VtnServiceId serviceId;
        private MacAddress mac;
        private IpAddress ip;
        // TODO remove this when XOS provides vSG information
        private Map<IpAddress, MacAddress> addressPairs;

        private Builder() {
        }

        /**
         * Builds an immutable VTN port.
         *
         * @return vtn port instance
         */
        public VtnPort build() {
            checkNotNull(id, "VTN port ID cannot be null");
            checkNotNull(serviceId, "VTN port service ID cannot be null");
            checkNotNull(mac, "VTN port MAC address cannot be null");
            checkNotNull(ip, "VTN port IP address cannot be null");
            addressPairs = addressPairs == null ? ImmutableMap.of() : addressPairs;

            return new VtnPort(id,
                               name,
                               serviceId,
                               mac,
                               ip,
                               addressPairs);
        }

        /**
         * Returns VTN port builder with the supplied port ID.
         *
         * @param id port identifier
         * @return vtn port builder
         */
        public Builder id(VtnPortId id) {
            this.id = id;
            return this;
        }

        /**
         * Returns VTN port builder with the supplied port name.
         * Port name can be null.
         *
         * @param name port name
         * @return vtn port builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Returns VTN port builder with the supplied service ID.
         *
         * @param serviceId vtn port service id
         * @return vtn port builder
         */
        public Builder serviceId(VtnServiceId serviceId) {
            this.serviceId = serviceId;
            return this;
        }

        /**
         * Returns VTN port builder with the supplied MAC address.
         *
         * @param mac mac address
         * @return vtn port builder
         */
        public Builder mac(MacAddress mac) {
            if (mac == null) {
                final String msg = "VTN port MAC address cannot be null";
                throw new IllegalArgumentException(msg);
            }
            this.mac = mac;
            return this;
        }

        /**
         * Returns VTN port builder with the supplied MAC address.
         *
         * @param mac mac address as a string
         * @return vtn port builder
         */
        public Builder mac(String mac) {
            try {
                return mac(MacAddress.valueOf(mac));
            } catch (IllegalArgumentException | NullPointerException e) {
                final String msg = "Malformed MAC address string " + mac +
                        " for VTN port MAC address";
                throw new IllegalArgumentException(msg);
            }
        }

        /**
         * Returns VTN port builder with the supplied IP address.
         *
         * @param ip ip address
         * @return vtn port builder
         */
        public Builder ip(IpAddress ip) {
            if (ip == null) {
                final String msg = "VTN port IP address cannot be null";
                throw new IllegalArgumentException(msg);
            }
            this.ip = ip;
            return this;
        }

        /**
         * Returns VTN port builder with the supplied IP address.
         *
         * @param ip ip address as a string
         * @return vtn port builder
         */
        public Builder ip(String ip) {
            try {
                return ip(IpAddress.valueOf(ip));
            } catch (IllegalArgumentException | NullPointerException e) {
                final String msg = "Malformed IP address string " + ip +
                        " for VTN port IP address";
                throw new IllegalArgumentException(msg);
            }
        }

        /**
         * Returns VTN port builder with the supplied address pairs.
         *
         * @param addressPairs address pairs
         * @return vtn port builder
         */
        public Builder addressPairs(Map<IpAddress, MacAddress> addressPairs) {
            if (addressPairs == null) {
                final String msg = "VTN address pairs cannot be null";
                throw new IllegalArgumentException(msg);
            }
            this.addressPairs = addressPairs;
            return this;
        }
    }
}
