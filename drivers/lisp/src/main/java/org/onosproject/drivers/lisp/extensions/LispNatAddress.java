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

package org.onosproject.drivers.lisp.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.flow.AbstractExtension;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.mapping.addresses.ExtensionMappingAddressType
                                .ExtensionMappingAddressTypes.NAT_ADDRESS;

/**
 * Implementation of LISP Network Address Translation (NAT) address.
 * When a LISP system is conveying global address and mapped port information
 * when traversing through a NAT device, the NAT-Traversal Type is used.
 */
public class LispNatAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {

    private static final String MS_UDP_PORT_NUMBER = "msUdpPortNumber";
    private static final String ETR_UDP_PORT_NUMBER = "etrUdpPortNumber";
    private static final String GLOBAL_ETR_RLOC_ADDRESS = "globalEtrRlocAddress";
    private static final String MS_RLOC_ADDRESS = "msRlocAddress";
    private static final String PRIVATE_ETR_RLOC_ADDRESS = "privateEtrRlocAddress";
    private static final String RTR_RLOC_ADDRESSES = "rtrRlocAddresses";

    private short msUdpPortNumber;
    private short etrUdpPortNumber;
    private MappingAddress globalEtrRlocAddress;
    private MappingAddress msRlocAddress;
    private MappingAddress privateEtrRlocAddress;
    private List<MappingAddress> rtrRlocAddresses;

    /**
     * Default constructor.
     */
    public LispNatAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param msUdpPortNumber       Map Server (MS) UDP port number
     * @param etrUdpPortNumber      ETR UDP port number
     * @param globalEtrRlocAddress  global ETR RLOC address
     * @param msRlocAddress         Map Server (MS) RLOC address
     * @param privateEtrRlocAddress private ETR RLOC address
     * @param rtrRlocAddresses      a collection of RTR RLOC addresses
     */
    private LispNatAddress(short msUdpPortNumber, short etrUdpPortNumber,
                           MappingAddress globalEtrRlocAddress,
                           MappingAddress msRlocAddress,
                           MappingAddress privateEtrRlocAddress,
                           List<MappingAddress> rtrRlocAddresses) {
        this.msUdpPortNumber = msUdpPortNumber;
        this.etrUdpPortNumber = etrUdpPortNumber;
        this.globalEtrRlocAddress = globalEtrRlocAddress;
        this.msRlocAddress = msRlocAddress;
        this.privateEtrRlocAddress = privateEtrRlocAddress;
        this.rtrRlocAddresses = ImmutableList.copyOf(rtrRlocAddresses);
    }

    /**
     * Obtains Map Server UDP port number.
     *
     * @return Map Server UDP port number
     */
    public short getMsUdpPortNumber() {
        return msUdpPortNumber;
    }

    /**
     * Obtains ETR UDP port number.
     *
     * @return ETR UDP port number
     */
    public short getEtrUdpPortNumber() {
        return etrUdpPortNumber;
    }

    /**
     * Obtains global ETR RLOC address.
     *
     * @return global ETR
     */
    public MappingAddress getGlobalEtrRlocAddress() {
        return globalEtrRlocAddress;
    }

    /**
     * Obtains Map Server RLOC address.
     *
     * @return Map Server RLOC address
     */
    public MappingAddress getMsRlocAddress() {
        return msRlocAddress;
    }

    /**
     * Obtains private ETR RLOC address.
     *
     * @return private ETR RLOC address
     */
    public MappingAddress getPrivateEtrRlocAddress() {
        return privateEtrRlocAddress;
    }

    /**
     * Obtains a collection of RTR RLOC addresses.
     *
     * @return a collection of RTR RLOC addresses
     */
    public List<MappingAddress> getRtrRlocAddresses() {
        return ImmutableList.copyOf(rtrRlocAddresses);
    }

    @Override
    public ExtensionMappingAddressType type() {
        return NAT_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();

        parameterMap.put(MS_UDP_PORT_NUMBER, msUdpPortNumber);
        parameterMap.put(ETR_UDP_PORT_NUMBER, etrUdpPortNumber);
        parameterMap.put(GLOBAL_ETR_RLOC_ADDRESS, globalEtrRlocAddress);
        parameterMap.put(MS_RLOC_ADDRESS, msRlocAddress);
        parameterMap.put(PRIVATE_ETR_RLOC_ADDRESS, privateEtrRlocAddress);
        parameterMap.put(RTR_RLOC_ADDRESSES, rtrRlocAddresses);

        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.msUdpPortNumber = (short) parameterMap.get(MS_UDP_PORT_NUMBER);
        this.etrUdpPortNumber = (short) parameterMap.get(ETR_UDP_PORT_NUMBER);
        this.globalEtrRlocAddress = (MappingAddress) parameterMap.get(GLOBAL_ETR_RLOC_ADDRESS);
        this.msRlocAddress = (MappingAddress) parameterMap.get(MS_RLOC_ADDRESS);
        this.privateEtrRlocAddress = (MappingAddress) parameterMap.get(PRIVATE_ETR_RLOC_ADDRESS);
        this.rtrRlocAddresses = (List<MappingAddress>) parameterMap.get(RTR_RLOC_ADDRESSES);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msUdpPortNumber, etrUdpPortNumber,
                globalEtrRlocAddress, msRlocAddress, privateEtrRlocAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispNatAddress) {
            final LispNatAddress other = (LispNatAddress) obj;
            return Objects.equals(this.msUdpPortNumber, other.msUdpPortNumber) &&
                    Objects.equals(this.etrUdpPortNumber, other.etrUdpPortNumber) &&
                    Objects.equals(this.globalEtrRlocAddress, other.globalEtrRlocAddress) &&
                    Objects.equals(this.msRlocAddress, other.msRlocAddress) &&
                    Objects.equals(this.privateEtrRlocAddress, other.privateEtrRlocAddress) &&
                    Objects.equals(this.rtrRlocAddresses, other.rtrRlocAddresses);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("Map Server UDP port number", msUdpPortNumber)
                .add("ETR UDP port number", etrUdpPortNumber)
                .add("global ETR RLOC address", globalEtrRlocAddress)
                .add("Map Server RLOC address", msRlocAddress)
                .add("private ETR RLOC address", privateEtrRlocAddress)
                .add("RTR RLOC addresses", rtrRlocAddresses)
                .toString();
    }

    /**
     * A builder for building LispNatAddress.
     */
    public static final class Builder {
        private short msUdpPortNumber;
        private short etrUdpPortNumber;
        private MappingAddress globalEtrRlocAddress;
        private MappingAddress msRlocAddress;
        private MappingAddress privateEtrRlocAddress;
        private List<MappingAddress> rtrRlocAddresses;

        /**
         * Sets Map Server UDP port number.
         *
         * @param msUdpPortNumber Map Server UDP port number
         * @return Builder object
         */
        public Builder withMsUdpPortNumber(short msUdpPortNumber) {
            this.msUdpPortNumber = msUdpPortNumber;
            return this;
        }

        /**
         * Sets ETR UDP port number.
         *
         * @param etrUdpPortNumber ETR UDP port number
         * @return Builder object
         */
        public Builder withEtrUdpPortNumber(short etrUdpPortNumber) {
            this.etrUdpPortNumber = etrUdpPortNumber;
            return this;
        }

        /**
         * Sets global ETR RLOC address.
         *
         * @param globalEtrRlocAddress global ETR RLOC address
         * @return Builder object
         */
        public Builder withGlobalEtrRlocAddress(MappingAddress globalEtrRlocAddress) {
            this.globalEtrRlocAddress = globalEtrRlocAddress;
            return this;
        }

        /**
         * Sets Map Server RLOC address.
         *
         * @param msRlocAddress Map Server RLOC address
         * @return Builder object
         */
        public Builder withMsRlocAddress(MappingAddress msRlocAddress) {
            this.msRlocAddress = msRlocAddress;
            return this;
        }

        /**
         * Sets private ETR RLOC address.
         *
         * @param privateEtrRlocAddress private ETR RLOC address
         * @return Builder object
         */
        public Builder withPrivateEtrRlocAddress(MappingAddress privateEtrRlocAddress) {
            this.privateEtrRlocAddress = privateEtrRlocAddress;
            return this;
        }

        /**
         * Sets RTR RLOC addresses.
         *
         * @param rtrRlocAddresses a collection of RTR RLOC addresses
         * @return Builder object
         */
        public Builder withRtrRlocAddresses(List<MappingAddress> rtrRlocAddresses) {
            if (rtrRlocAddresses != null) {
                this.rtrRlocAddresses = ImmutableList.copyOf(rtrRlocAddresses);
            }
            return this;
        }

        /**
         * Builds LispNatAddress instance.
         *
         * @return LispNatAddress instance
         */
        public LispNatAddress build() {

            return new LispNatAddress(msUdpPortNumber, etrUdpPortNumber,
                                        globalEtrRlocAddress, msRlocAddress,
                                        privateEtrRlocAddress, rtrRlocAddresses);
        }
    }
}
