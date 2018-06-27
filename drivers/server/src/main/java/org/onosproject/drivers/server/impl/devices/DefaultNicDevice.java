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

import org.onlab.packet.MacAddress;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static org.onosproject.net.Port.Type;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation for NIC devices.
 */
public class DefaultNicDevice implements NicDevice, Comparable {

    private final String     id;
    private final int        port;
    private final long       speed;
    private final Type       portType;
    private boolean          status;
    private final MacAddress macAddress;
    private NicRxFilter      rxFilterMechanisms;

    // 200 Gbps or 200.000 Mbps
    public static final long MAX_SPEED = 200000;

    public DefaultNicDevice(
            String      id,
            int         port,
            Type        portType,
            long        speed,
            boolean     status,
            String      macStr,
            NicRxFilter rxFilterMechanisms) {
        checkNotNull(id, "NIC ID cannot be null");
        checkArgument(!id.isEmpty(), "NIC ID cannot be empty");
        checkArgument(port >= 0, "NIC port number must be non-negative");
        checkNotNull(portType, "NIC port type cannot be null");
        checkArgument(
            (speed >= 0) && (speed <= MAX_SPEED),
            "NIC speed must be positive and less or equal than " + MAX_SPEED + " Mbps"
        );
        checkNotNull(macStr, "NIC MAC address cannot be null");
        checkNotNull(rxFilterMechanisms, "NIC Rx filter mechanisms cannot be null");

        // Implies a problem
        if (speed == 0) {
            status = false;
        }

        this.id         = id;
        this.port       = port;
        this.speed      = speed;
        this.portType   = portType;
        this.status     = status;
        this.macAddress = MacAddress.valueOf(macStr);
        this.rxFilterMechanisms  = rxFilterMechanisms;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public int port() {
        return this.port;
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
    public void addRxFilterMechanism(NicRxFilter.RxFilter rxFilter) {
        this.rxFilterMechanisms.addRxFilter(rxFilter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id",        id())
                .add("port",      port())
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
        return  this.id().equals(device.id()) &&
                this.port() ==  device.port() &&
                this.speed  == device.speed() &&
                this.macAddress.equals(device.macAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, port, speed, macAddress);
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

            if (this.port() == otherNic.port()) {
                return 0;
            } else if (this.port() > otherNic.port()) {
                return 1;
            } else {
                return -1;
            }
        }

        return -1;
    }

}
