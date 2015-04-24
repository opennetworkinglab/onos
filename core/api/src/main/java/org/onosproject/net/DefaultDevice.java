/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net;

import org.onosproject.net.provider.ProviderId;
import org.onlab.packet.ChassisId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default infrastructure device model implementation.
 */
public class DefaultDevice extends AbstractElement implements Device {

    private final Type type;
    private final String manufacturer;
    private final String serialNumber;
    private final String hwVersion;
    private final String swVersion;
    private final ChassisId chassisId;

    // For serialization
    private DefaultDevice() {
        this.type = null;
        this.manufacturer = null;
        this.hwVersion = null;
        this.swVersion = null;
        this.serialNumber = null;
        this.chassisId = null;
    }

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param providerId   identity of the provider
     * @param id           device identifier
     * @param type         device type
     * @param manufacturer device manufacturer
     * @param hwVersion    device HW version
     * @param swVersion    device SW version
     * @param serialNumber device serial number
     * @param chassisId    chassis id
     * @param annotations  optional key/value annotations
     */
    public DefaultDevice(ProviderId providerId, DeviceId id, Type type,
                         String manufacturer, String hwVersion, String swVersion,
                         String serialNumber, ChassisId chassisId,
                         Annotations... annotations) {
        super(providerId, id, annotations);
        this.type = type;
        this.manufacturer = manufacturer;
        this.hwVersion = hwVersion;
        this.swVersion = swVersion;
        this.serialNumber = serialNumber;
        this.chassisId = chassisId;
    }

    @Override
    public DeviceId id() {
        return (DeviceId) id;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String manufacturer() {
        return manufacturer;
    }

    @Override
    public String hwVersion() {
        return hwVersion;
    }

    @Override
    public String swVersion() {
        return swVersion;
    }

    @Override
    public String serialNumber() {
        return serialNumber;
    }

    @Override
    public ChassisId chassisId() {
        return chassisId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, manufacturer, hwVersion, swVersion, serialNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultDevice) {
            final DefaultDevice other = (DefaultDevice) obj;
            return Objects.equals(this.id, other.id) &&
                    Objects.equals(this.type, other.type) &&
                    Objects.equals(this.manufacturer, other.manufacturer) &&
                    Objects.equals(this.hwVersion, other.hwVersion) &&
                    Objects.equals(this.swVersion, other.swVersion) &&
                    Objects.equals(this.serialNumber, other.serialNumber);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("type", type)
                .add("manufacturer", manufacturer)
                .add("hwVersion", hwVersion)
                .add("swVersion", swVersion)
                .add("serialNumber", serialNumber)
                .toString();
    }

}
