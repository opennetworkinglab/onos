/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onlab.packet.ChassisId;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.DefaultDriverHandler;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default infrastructure device model implementation.
 */
public class DefaultDevice extends AbstractElement implements Device {

    private static final int MANUFACTURER_MAX_LENGTH = 256;
    private static final int HW_VERSION_MAX_LENGTH = 256;
    private static final int SW_VERSION_MAX_LENGTH = 256;
    private static final int SERIAL_NUMBER_MAX_LENGTH = 256;

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
        if (hwVersion != null) {
            checkArgument(hwVersion.length() <= HW_VERSION_MAX_LENGTH,
                    "hwVersion exceeds maximum length " + HW_VERSION_MAX_LENGTH);
        }
        if (swVersion != null) {
            checkArgument(swVersion.length() <= SW_VERSION_MAX_LENGTH,
                    "swVersion exceeds maximum length " + SW_VERSION_MAX_LENGTH);
        }
        if (manufacturer != null) {
            checkArgument(manufacturer.length() <= MANUFACTURER_MAX_LENGTH,
                    "manufacturer exceeds maximum length " + MANUFACTURER_MAX_LENGTH);
        }
        if (serialNumber != null) {
            checkArgument(serialNumber.length() <= SERIAL_NUMBER_MAX_LENGTH,
                    "serialNumber exceeds maximum length " + SERIAL_NUMBER_MAX_LENGTH);
        }
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
    public <B extends Behaviour> B as(Class<B> projectionClass) {
        if (HandlerBehaviour.class.isAssignableFrom(projectionClass)) {
            bindAndCheckDriver();
            return driver().createBehaviour(new DefaultDriverHandler(asData()), projectionClass);
        }
        return super.as(projectionClass);
    }

    /**
     * Returns self as an immutable driver data instance.
     *
     * @return self as driver data
     */
    protected DriverData asData() {
        return new DeviceDriverData();
    }

    @Override
    protected Driver locateDriver() {
        Driver driver = super.locateDriver();
        return driver != null ? driver :
                driverService().getDriver(manufacturer, hwVersion, swVersion);
    }

    /**
     * Projection of the parent entity as a driver data entity.
     */
    protected class DeviceDriverData extends AnnotationDriverData {
        @Override
        public DeviceId deviceId() {
            return id();
        }
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
                .add("driver", driver() != null ? driver().name() : "")
                .toString();
    }

}
