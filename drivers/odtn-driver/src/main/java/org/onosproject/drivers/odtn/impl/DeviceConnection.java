/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.net.flow.FlowRule;

import java.util.Objects;

/**
 * Connection consisting of a unique identifier of the connection on the device and the corresponding rule within ONOS.
 */
public final class DeviceConnection {

    private String id;
    private FlowRule fr;

    //Avoiding public construction
    private DeviceConnection(){}

    /**
     * Creates the Device connection object.
     *
     * @param id the unique id of the connection on the device
     * @param fr the flow rule in ONOS
     */
    private DeviceConnection(String id, FlowRule fr) {
        this.id = id;
        this.fr = fr;
    }

    /**
     * Creates the Device connection object.
     *
     * @param id the unique id of the connection on the device
     * @param fr the flow rule in ONOS
     * @return the DeviceConnection object.
     */
    public static DeviceConnection of(String id, FlowRule fr) {
        return new DeviceConnection(id, fr);
    }

    /**
     * Gets the unique id of the connection on the device.
     * E.g TAPI UUID of the connectivity service.
     *
     * @return the unique id on the device.
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the flow rule associated to the unique id on the device.
     * The Flow Rule contains the info of the given connection as needed by ONOS.
     *
     * @return the flow rule
     */
    public FlowRule getFlowRule() {
        return fr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceConnection that = (DeviceConnection) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fr);
    }

     @Override
     public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("fr", fr)
                .toString();
     }
}
