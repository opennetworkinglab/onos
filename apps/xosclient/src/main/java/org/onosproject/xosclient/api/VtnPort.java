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
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import java.util.Map;
import java.util.Objects;

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


    /**
     * Creates a new vtn port with the specified entities.
     *
     * @param id vtn port id
     * @param name vtn port name
     * @param serviceId id of the service this port is in
     * @param mac mac address
     * @param ip ip address
     * @param addressPairs ip and mac pairs of nested container
     */
    public VtnPort(VtnPortId id,
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
}
