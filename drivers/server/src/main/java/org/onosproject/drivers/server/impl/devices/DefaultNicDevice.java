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

package org.onosproject.drivers.server.impl.devices;

import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.nic.NicRxFilter;
import org.onosproject.drivers.server.devices.nic.NicRxFilter.RxFilter;
import org.onosproject.net.Port;

import org.onlab.packet.MacAddress;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.onosproject.net.Port.Type;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_NIC_NAME_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_MAC_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_PORT_NUMBER_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_NIC_PORT_TYPE_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_RX_FILTER_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_RX_FILTERS_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_SPEED_NEGATIVE;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_PORT_TYPE_COPPER;
import static org.onosproject.drivers.server.Constants.PARAM_NIC_PORT_TYPE_FIBER;

/**
 * Default implementation for NIC devices.
 */
public final class DefaultNicDevice implements NicDevice, Comparable {

    private final String     name;
    private final long       portNumber;
    private final Type       portType;
    private final long       speed;
    private boolean          status;
    private final MacAddress macAddress;
    private NicRxFilter      rxFilterMechanisms;

    private DefaultNicDevice(
            String      name,
            long        portNumber,
            Type        portType,
            long        speed,
            boolean     status,
            MacAddress  mac,
            NicRxFilter rxFilterMechanisms) {
        checkArgument(!Strings.isNullOrEmpty(name), MSG_NIC_NAME_NULL);
        checkArgument(portNumber >= 0, MSG_NIC_PORT_NUMBER_NEGATIVE);
        checkNotNull(portType, MSG_NIC_PORT_TYPE_NULL);
        checkArgument((speed >= 0) && (speed <= NicDevice.MAX_SPEED),
            MSG_NIC_SPEED_NEGATIVE);
        checkNotNull(mac, MSG_NIC_MAC_NULL);
        checkNotNull(rxFilterMechanisms, MSG_NIC_RX_FILTERS_NULL);

        // Implies a problem
        if (speed == 0) {
            status = false;
        }

        this.name       = name;
        this.portNumber = portNumber;
        this.speed      = speed;
        this.portType   = portType;
        this.status     = status;
        this.macAddress = mac;
        this.rxFilterMechanisms = rxFilterMechanisms;
    }

    /**
     * Creates a builder for DefaultNicDevice object.
     *
     * @return builder object for DefaultNicDevice object
     */
    public static DefaultNicDevice.Builder builder() {
        return new Builder();
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public long portNumber() {
        return this.portNumber;
    }

    @Override
    public Type portType() {
        return this.portType;
    }

    @Override
    public long speed() {
        return this.speed;
    }

    @Override
    public boolean status() {
        return this.status;
    }

    @Override
    public void setStatus(boolean status) {
        this.status = status;
    }

    @Override
    public MacAddress macAddress() {
        return this.macAddress;
    }

    @Override
    public NicRxFilter rxFilterMechanisms() {
        return this.rxFilterMechanisms;
    }

    @Override
    public void setRxFilterMechanisms(NicRxFilter rxFilterMechanisms) {
        checkNotNull(rxFilterMechanisms, "NIC Rx filter mechanisms cannot be null");
        this.rxFilterMechanisms = rxFilterMechanisms;
    }

    @Override
    public void addRxFilterMechanism(RxFilter rxFilter) {
        this.rxFilterMechanisms.addRxFilter(rxFilter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("name",      name())
                .add("port",      portNumber())
                .add("mac",       macAddress.toString())
                .add("portType",  portType())
                .add("speed",     speed())
                .add("status",    status ? "active" : "inactive")
                .add("rxFilters", rxFilterMechanisms())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NicDevice)) {
            return false;
        }
        NicDevice device = (NicDevice) obj;
        return  this.name().equals(device.name()) &&
                this.portNumber() ==  device.portNumber() &&
                this.speed  == device.speed() &&
                this.macAddress.equals(device.macAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, portNumber, speed, macAddress);
    }

    @Override
    public int compareTo(Object other) {
        if (this == other) {
            return 0;
        }

        if (other == null) {
            return -1;
        }

        if (other instanceof NicDevice) {
            NicDevice otherNic = (NicDevice) other;

            if (this.portNumber() == otherNic.portNumber()) {
                return 0;
            } else if (this.portNumber() > otherNic.portNumber()) {
                return 1;
            } else {
                return -1;
            }
        }

        return -1;
    }

    public static final class Builder {
        String      name;
        long        portNumber = -1;
        Type        portType = Type.FIBER;
        long        speed = -1;
        boolean     status = false;
        MacAddress  macAddress = MacAddress.ZERO;
        NicRxFilter rxFilterMechanisms = new NicRxFilter();

        /**
         * Port types that usually appear in commodity servers.
         */
        static final Map<String, Port.Type> PORT_TYPE_MAP =
            Collections.unmodifiableMap(
                new HashMap<String, Port.Type>() {
                    {
                        put(PARAM_NIC_PORT_TYPE_COPPER, Port.Type.COPPER);
                        put(PARAM_NIC_PORT_TYPE_FIBER,  Port.Type.FIBER);
                    }
                }
        );

        private Builder() {

        }

        /**
         * Sets the name of this NIC.
         *
         * @param name NIC name
         * @return builder object
         */
        public Builder setName(String name) {
            this.name = name;

            return this;
        }

        /**
         * Sets the NIC's port number.
         *
         * @param portNumber NIC's port number
         * @return builder object
         */
        public Builder setPortNumber(long portNumber) {
            this.portNumber = portNumber;

            return this;
        }

        /**
         * Sets the NIC's port type as a string.
         *
         * @param portTypeStr NIC's port type
         * @return builder object
         */
        public Builder setPortType(String portTypeStr) {
            portType = PORT_TYPE_MAP.get(portTypeStr);
            if (portType == null) {
                throw new IllegalArgumentException(
                    portTypeStr + " is not a valid NIC port type");
            }

            return this;
        }

        /**
         * Sets the NIC's speed.
         *
         * @param speed NIC's speed
         * @return builder object
         */
        public Builder setSpeed(long speed) {
            this.speed = speed;

            return this;
        }

        /**
         * Sets the NIC's status.
         *
         * @param status NIC's status
         * @return builder object
         */
        public Builder setStatus(boolean status) {
            this.status = status;

            return this;
        }

        /**
         * Sets the NIC's MAC address.
         *
         * @param macAddressStr NIC's MAC address
         * @return builder object
         */
        public Builder setMacAddress(String macAddressStr) {
            this.macAddress = MacAddress.valueOf(macAddressStr);

            return this;
        }

        /**
         * Sets the NIC's list of Rx filters as strings.
         *
         * @param rxFilters NIC's list of Rx filters
         * @return builder object
         */
        public Builder setRxFilters(List<String> rxFilters) {
            checkNotNull(rxFilters, MSG_NIC_RX_FILTERS_NULL);
            for (String s : rxFilters) {
                // Verify that this is a valid Rx filter
                RxFilter rf = RxFilter.getByName(s);
                checkNotNull(rf, MSG_NIC_RX_FILTER_NULL);
                this.rxFilterMechanisms.addRxFilter(rf);
            }

            return this;
        }

        /**
         * Creates a DefaultNicDevice object.
         *
         * @return DefaultNicDevice object
         */
        public DefaultNicDevice build() {
            return new DefaultNicDevice(
                name, portNumber, portType, speed,
                status, macAddress, rxFilterMechanisms);
        }

    }

}
