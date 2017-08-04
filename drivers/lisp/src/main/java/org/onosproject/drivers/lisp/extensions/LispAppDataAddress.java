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

import com.google.common.collect.Maps;
import org.onosproject.mapping.addresses.ExtensionMappingAddress;
import org.onosproject.mapping.addresses.ExtensionMappingAddressType;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.net.flow.AbstractExtension;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.mapping.addresses.ExtensionMappingAddressType
                        .ExtensionMappingAddressTypes.APPLICATION_DATA_ADDRESS;

/**
 * Implementation of LISP application data address.
 * When a locator-set needs to be conveyed based on the type of application or
 * the Per-Hop Behavior (PHB) of a packet, the Application Data Type can be used.
 */
public final class LispAppDataAddress extends AbstractExtension
                                            implements ExtensionMappingAddress {

    private static final String PROTOCOL = "protocol";
    private static final String IP_TOS = "ipTos";
    private static final String LOCAL_PORT_LOW = "localPortLow";
    private static final String LOCAL_PORT_HIGH = "localPortHigh";
    private static final String REMOTE_PORT_LOW = "remotePortLow";
    private static final String REMOTE_PORT_HIGH = "remotePortHigh";
    private static final String ADDRESS = "address";

    private byte protocol;
    private int ipTos;
    private short localPortLow;
    private short localPortHigh;
    private short remotePortLow;
    private short remotePortHigh;
    private MappingAddress address;

    /**
     * Default constructor.
     */
    public LispAppDataAddress() {
    }

    /**
     * Creates an instance with initialized parameters.
     *
     * @param protocol       protocol number
     * @param ipTos          IP type of service
     * @param localPortLow   low-ranged local port number
     * @param localPortHigh  high-ranged local port number
     * @param remotePortLow  low-ranged remote port number
     * @param remotePortHigh high-ranged remote port number
     */
    private LispAppDataAddress(byte protocol, int ipTos, short localPortLow,
                              short localPortHigh, short remotePortLow,
                              short remotePortHigh, MappingAddress address) {
        this.protocol = protocol;
        this.ipTos = ipTos;
        this.localPortLow = localPortLow;
        this.localPortHigh = localPortHigh;
        this.remotePortLow = remotePortLow;
        this.remotePortHigh = remotePortHigh;
        this.address = address;
    }

    /**
     * Obtains protocol type.
     *
     * @return protocol type
     */
    public byte getProtocol() {
        return protocol;
    }

    /**
     * Obtains IP type of service.
     *
     * @return IP type of service
     */
    public int getIpTos() {
        return ipTos;
    }

    /**
     * Obtains local port low.
     *
     * @return local port low
     */
    public short getLocalPortLow() {
        return localPortLow;
    }

    /**
     * Obtains local port high.
     *
     * @return local port high
     */
    public short getLocalPortHigh() {
        return localPortHigh;
    }

    /**
     * Obtains remote port low.
     *
     * @return remote port low
     */
    public short getRemotePortLow() {
        return remotePortLow;
    }

    /**
     * Obtains remote port high.
     *
     * @return remote port high
     */
    public short getRemotePortHigh() {
        return remotePortHigh;
    }

    /**
     * Obtains mapping address.
     *
     * @return mapping address
     */
    public MappingAddress getAddress() {
        return address;
    }

    @Override
    public ExtensionMappingAddressType type() {
        return APPLICATION_DATA_ADDRESS.type();
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> parameterMap = Maps.newHashMap();
        parameterMap.put(PROTOCOL, protocol);
        parameterMap.put(IP_TOS, ipTos);
        parameterMap.put(LOCAL_PORT_LOW, localPortLow);
        parameterMap.put(LOCAL_PORT_HIGH, localPortHigh);
        parameterMap.put(REMOTE_PORT_LOW, remotePortLow);
        parameterMap.put(REMOTE_PORT_HIGH, remotePortHigh);
        parameterMap.put(ADDRESS, address);

        return APP_KRYO.serialize(parameterMap);
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> parameterMap = APP_KRYO.deserialize(data);

        this.protocol = (byte) parameterMap.get(PROTOCOL);
        this.ipTos = (int) parameterMap.get(IP_TOS);
        this.localPortLow = (short) parameterMap.get(LOCAL_PORT_LOW);
        this.localPortHigh = (short) parameterMap.get(LOCAL_PORT_HIGH);
        this.remotePortLow = (short) parameterMap.get(REMOTE_PORT_LOW);
        this.remotePortHigh = (short) parameterMap.get(REMOTE_PORT_HIGH);
        this.address = (MappingAddress) parameterMap.get(ADDRESS);
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocol, ipTos, localPortLow, localPortHigh,
                remotePortLow, remotePortHigh, address);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LispAppDataAddress) {
            LispAppDataAddress that = (LispAppDataAddress) obj;
            return Objects.equals(protocol, that.protocol) &&
                    Objects.equals(ipTos, that.ipTos) &&
                    Objects.equals(localPortLow, that.localPortLow) &&
                    Objects.equals(localPortHigh, that.localPortHigh) &&
                    Objects.equals(remotePortLow, that.remotePortLow) &&
                    Objects.equals(remotePortHigh, that.remotePortHigh) &&
                    Objects.equals(address, that.address);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("protocol", protocol)
                .add("IP type of service", ipTos)
                .add("low-ranged local port number", localPortLow)
                .add("high-ranged local port number", localPortHigh)
                .add("low-ranged remote port number", remotePortLow)
                .add("high-ranged remote port number", remotePortHigh)
                .add("address", address)
                .toString();
    }

    /**
     * A builder for building LispAppDataAddress.
     */
    public static final class Builder {
        private byte protocol;
        private int ipTos;
        private short localPortLow;
        private short localPortHigh;
        private short remotePortLow;
        private short remotePortHigh;
        private MappingAddress address;

        /**
         * Sets protocol number.
         *
         * @param protocol protocol number
         * @return Builder object
         */
        public Builder withProtocol(byte protocol) {
            this.protocol = protocol;
            return this;
        }

        /**
         * Sets IP type of service.
         *
         * @param ipTos IP type of service
         * @return Builder object
         */
        public Builder withIpTos(int ipTos) {
            this.ipTos = ipTos;
            return this;
        }

        /**
         * Sets low-ranged local port number.
         *
         * @param localPortLow low-ranged local port number
         * @return Builder object
         */
        public Builder withLocalPortLow(short localPortLow) {
            this.localPortLow = localPortLow;
            return this;
        }

        /**
         * Sets high-ranged local port number.
         *
         * @param localPortHigh high-ranged local port number
         * @return Builder object
         */
        public Builder withLocalPortHigh(short localPortHigh) {
            this.localPortHigh = localPortHigh;
            return this;
        }

        /**
         * Sets low-ranged remote port number.
         *
         * @param remotePortLow low-ranged remote port number
         * @return Builder object
         */
        public Builder withRemotePortLow(short remotePortLow) {
            this.remotePortLow = remotePortLow;
            return this;
        }

        /**
         * Sets high-ranged remote port number.
         *
         * @param remotePortHigh high-ranged remote port number
         * @return Builder object
         */
        public Builder withRemotePortHigh(short remotePortHigh) {
            this.remotePortHigh = remotePortHigh;
            return this;
        }

        /**
         * Sets mapping address.
         *
         * @param address mapping address
         * @return Builder object
         */
        public Builder withAddress(MappingAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Builds LispAppDataLcafAddress instance.
         *
         * @return LispAddDataLcafAddress instance
         */
        public LispAppDataAddress build() {

            return new LispAppDataAddress(protocol, ipTos, localPortLow,
                    localPortHigh, remotePortLow, remotePortHigh, address);
        }
    }
}
