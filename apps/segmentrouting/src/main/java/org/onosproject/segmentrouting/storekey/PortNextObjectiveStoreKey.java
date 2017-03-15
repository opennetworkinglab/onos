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
package org.onosproject.segmentrouting.storekey;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Objects;

/**
 * Key of Device/Port to NextObjective store.
 *
 * Since there can be multiple next objectives to the same physical port,
 * we differentiate between them by including the treatment in the key.
 */
public class PortNextObjectiveStoreKey {
    private final DeviceId deviceId;
    private final PortNumber portNum;
    private final TrafficTreatment treatment;
    private final TrafficSelector meta;

    /**
     * Constructs the key of port next objective store.
     *
     * @param deviceId device ID
     * @param portNum port number
     * @param treatment treatment that will be applied to the interface
     * @param meta optional data to pass to the driver
     */
    public PortNextObjectiveStoreKey(DeviceId deviceId, PortNumber portNum,
                                     TrafficTreatment treatment,
                                     TrafficSelector meta) {
        this.deviceId = deviceId;
        this.portNum = portNum;
        this.treatment = treatment;
        this.meta = meta;
    }

    /**
     * Gets device id in this PortNextObjectiveStoreKey.
     *
     * @return device id
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Gets port information in this PortNextObjectiveStoreKey.
     *
     * @return port information
     */
    public PortNumber portNumber() {
        return portNum;
    }

    /**
     * Gets treatment information in this PortNextObjectiveStoreKey.
     *
     * @return treatment information
     */
    public TrafficTreatment treatment() {
        return treatment;
    }

    /**
     * Gets metadata information in this PortNextObjectiveStoreKey.
     *
     * @return meta information
     */
    public TrafficSelector meta() {
        return meta;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortNextObjectiveStoreKey)) {
            return false;
        }
        PortNextObjectiveStoreKey that =
                (PortNextObjectiveStoreKey) o;
        return (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.portNum, that.portNum) &&
                Objects.equals(this.treatment, that.treatment) &&
                Objects.equals(this.meta, that.meta));
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, portNum, treatment, meta);
    }

    @Override
    public String toString() {
        return "Device: " + deviceId + " Port: " + portNum +
                " Treatment: " + treatment +
                " Meta: " + meta;
    }
}
