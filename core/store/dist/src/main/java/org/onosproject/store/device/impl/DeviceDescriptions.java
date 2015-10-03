/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DefaultAnnotations.union;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.OchPortDescription;
import org.onosproject.net.device.OduCltPortDescription;
import org.onosproject.net.device.OmsPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.store.Timestamp;
import org.onosproject.store.impl.Timestamped;

/*
 * Collection of Description of a Device and Ports, given from a Provider.
 */
class DeviceDescriptions {

    private volatile Timestamped<DeviceDescription> deviceDesc;

    private final ConcurrentMap<PortNumber, Timestamped<PortDescription>> portDescs;

    public DeviceDescriptions(Timestamped<DeviceDescription> desc) {
        this.deviceDesc = checkNotNull(desc);
        this.portDescs = new ConcurrentHashMap<>();
    }

    public Timestamp getLatestTimestamp() {
        Timestamp latest = deviceDesc.timestamp();
        for (Timestamped<PortDescription> desc : portDescs.values()) {
            if (desc.timestamp().compareTo(latest) > 0) {
                latest = desc.timestamp();
            }
        }
        return latest;
    }

    public Timestamped<DeviceDescription> getDeviceDesc() {
        return deviceDesc;
    }

    public Timestamped<PortDescription> getPortDesc(PortNumber number) {
        return portDescs.get(number);
    }

    public Map<PortNumber, Timestamped<PortDescription>> getPortDescs() {
        return Collections.unmodifiableMap(portDescs);
    }

    /**
     * Puts DeviceDescription, merging annotations as necessary.
     *
     * @param newDesc new DeviceDescription
     */
    public void putDeviceDesc(Timestamped<DeviceDescription> newDesc) {
        Timestamped<DeviceDescription> oldOne = deviceDesc;
        Timestamped<DeviceDescription> newOne = newDesc;
        if (oldOne != null) {
            SparseAnnotations merged = union(oldOne.value().annotations(),
                                             newDesc.value().annotations());
            newOne = new Timestamped<>(
                    new DefaultDeviceDescription(newDesc.value(), merged),
                    newDesc.timestamp());
        }
        deviceDesc = newOne;
    }

    /**
     * Puts PortDescription, merging annotations as necessary.
     *
     * @param newDesc new PortDescription
     */
    public void putPortDesc(Timestamped<PortDescription> newDesc) {
        Timestamped<PortDescription> oldOne = portDescs.get(newDesc.value().portNumber());
        Timestamped<PortDescription> newOne = newDesc;
        if (oldOne != null) {
            SparseAnnotations merged = union(oldOne.value().annotations(),
                                             newDesc.value().annotations());
            newOne = null;
            switch (newDesc.value().type()) {
                case OMS:
                    OmsPortDescription omsDesc = (OmsPortDescription) (newDesc.value());
                    newOne = new Timestamped<>(
                            new OmsPortDescription(
                                    omsDesc, omsDesc.minFrequency(), omsDesc.maxFrequency(), omsDesc.grid(), merged),
                            newDesc.timestamp());
                    break;
                case OCH:
                    OchPortDescription ochDesc = (OchPortDescription) (newDesc.value());
                    newOne = new Timestamped<>(
                            new OchPortDescription(
                                    ochDesc, ochDesc.signalType(), ochDesc.isTunable(), ochDesc.lambda(), merged),
                            newDesc.timestamp());
                    break;
                case ODUCLT:
                    OduCltPortDescription ocDesc = (OduCltPortDescription) (newDesc.value());
                    newOne = new Timestamped<>(
                            new OduCltPortDescription(
                                    ocDesc, ocDesc.signalType(), merged),
                            newDesc.timestamp());
                    break;
                default:
                    newOne = new Timestamped<>(
                            new DefaultPortDescription(newDesc.value(), merged),
                            newDesc.timestamp());
            }
        }
        portDescs.put(newOne.value().portNumber(), newOne);
    }
}
