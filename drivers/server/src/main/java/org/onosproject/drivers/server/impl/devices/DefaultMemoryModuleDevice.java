/*
 * Copyright 2020-present Open Networking Foundation
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

import org.onosproject.drivers.server.devices.memory.MemoryModuleDevice;
import org.onosproject.drivers.server.devices.memory.MemoryType;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_MEM_MANUFACTURER_NULL;
import static org.onosproject.drivers.server.Constants.MSG_MEM_SERIAL_NB_NULL;
import static org.onosproject.drivers.server.Constants.MSG_MEM_SIZE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_MEM_SPEED_CONF_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_MEM_SPEED_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_MEM_TYPE_NULL;
import static org.onosproject.drivers.server.Constants.MSG_MEM_WIDTH_DATA_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_MEM_WIDTH_TOTAL_NEGATIVE;
import static org.onosproject.drivers.server.devices.memory.MemoryModuleDevice.MAX_SPEED_MTS;

/**
 * Default implementation for main memory module devices.
 */
public final class DefaultMemoryModuleDevice implements MemoryModuleDevice {

    private final MemoryType type;
    private final String manufacturer;
    private final String serialNumber;
    private final int dataWidth;
    private final int totalWidth;
    private final long capacity;
    private final long speed;
    private final long configuredSpeed;

    private DefaultMemoryModuleDevice(MemoryType type,
                                      String manufacturer,
                                      String serialNumber,
                                      int dataWidth,
                                      int totalWidth,
                                      long capacity,
                                      long speed,
                                      long configuredSpeed) {
        checkNotNull(type, MSG_MEM_TYPE_NULL);
        checkNotNull(manufacturer, MSG_MEM_MANUFACTURER_NULL);
        checkNotNull(serialNumber, MSG_MEM_SERIAL_NB_NULL);
        checkArgument(dataWidth > 0, MSG_MEM_WIDTH_DATA_NEGATIVE);
        checkArgument(totalWidth > 0, MSG_MEM_WIDTH_TOTAL_NEGATIVE);
        checkArgument(capacity > 0, MSG_MEM_SIZE_NEGATIVE);
        checkArgument((speed > 0) && (speed <= MAX_SPEED_MTS), MSG_MEM_SPEED_NEGATIVE);
        checkArgument((configuredSpeed > 0) && (configuredSpeed <= speed), MSG_MEM_SPEED_CONF_NEGATIVE);

        this.type = type;
        this.manufacturer = manufacturer;
        this.serialNumber = serialNumber;
        this.dataWidth = dataWidth;
        this.totalWidth = totalWidth;
        this.capacity = capacity;
        this.speed = speed;
        this.configuredSpeed = configuredSpeed;
    }

    /**
     * Creates a builder for DefaultMemoryModuleDevice object.
     *
     * @return builder object for DefaultMemoryModuleDevice object
     */
    public static DefaultMemoryModuleDevice.Builder builder() {
        return new Builder();
    }

    @Override
    public MemoryType type() {
        return this.type;
    }

    @Override
    public String manufacturer() {
        return this.manufacturer;
    }

    @Override
    public String serialNumber() {
        return this.serialNumber;
    }

    @Override
    public int dataWidth() {
        return this.dataWidth;
    }

    @Override
    public int totalWidth() {
        return this.totalWidth;
    }

    @Override
    public long capacity() {
        return this.capacity;
    }

    @Override
    public long speed() {
        return this.speed;
    }

    @Override
    public long configuredSpeed() {
        return this.configuredSpeed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("type", type())
                .add("manufacturer", manufacturer())
                .add("serialNumber", serialNumber())
                .add("dataWidth", dataWidth())
                .add("totalWidth", totalWidth())
                .add("capacity", capacity())
                .add("speed", speed())
                .add("configuredSpeed", configuredSpeed())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MemoryModuleDevice)) {
            return false;
        }
        MemoryModuleDevice device = (MemoryModuleDevice) obj;
        return  this.type() ==  device.type() &&
                this.manufacturer() == device.manufacturer() &&
                this.serialNumber() == device.serialNumber() &&
                this.dataWidth() == device.dataWidth() &&
                this.totalWidth() == device.totalWidth() &&
                this.capacity() == device.capacity() &&
                this.speed() == device.speed() &&
                this.configuredSpeed() == device.configuredSpeed();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, manufacturer, serialNumber,
            dataWidth, totalWidth, capacity, speed, configuredSpeed);
    }

    public static final class Builder {
        MemoryType type = null;
        String manufacturer = null;
        String serialNumber = null;
        int dataWidth = -1;
        int totalWidth = -1;
        long capacity = -1;
        long speed = -1;
        long configuredSpeed = -1;

        private Builder() {

        }

        /**
         * Sets the type of this memory module.
         *
         * @param typeStr memory module's type
         * @return builder object
         */
        public Builder setType(String typeStr) {
            if (!Strings.isNullOrEmpty(typeStr)) {
                this.type = MemoryType.getByName(typeStr);
            }

            return this;
        }

        /**
         * Sets the manufacturer of this memory module.
         *
         * @param manufacturer memory module's manufacturer
         * @return builder object
         */
        public Builder setManufacturer(String manufacturer) {
            if (!Strings.isNullOrEmpty(manufacturer)) {
                this.manufacturer = manufacturer;
            }

            return this;
        }

        /**
         * Sets the serial number of this memory module.
         *
         * @param serialNumber memory module's serial number
         * @return builder object
         */
        public Builder setSerialNumber(String serialNumber) {
            if (!Strings.isNullOrEmpty(serialNumber)) {
                this.serialNumber = serialNumber;
            }

            return this;
        }

        /**
         * Sets this memory module's data width.
         *
         * @param dataWidth memory's data width
         * @return builder object
         */
        public Builder setDataWidth(int dataWidth) {
            this.dataWidth = dataWidth;

            return this;
        }

        /**
         * Sets this memory module's total width.
         *
         * @param totalWidth memory's total width
         * @return builder object
         */
        public Builder setTotalWidth(int totalWidth) {
            this.totalWidth = totalWidth;

            return this;
        }

        /**
         * Sets this memory module's capacity in MBytes.
         *
         * @param capacity memory's capacity
         * @return builder object
         */
        public Builder setCapacity(long capacity) {
            this.capacity = capacity;

            return this;
        }

        /**
         * Sets the speed of this memory module.
         *
         * @param speed memory speed
         * @return builder object
         */
        public Builder setSpeed(long speed) {
            this.speed = speed;

            return this;
        }

        /**
         * Sets the configured speed of this memory module.
         *
         * @param configuredSpeed memory's configured speed
         * @return builder object
         */
        public Builder setConfiguredSpeed(long configuredSpeed) {
            this.configuredSpeed = configuredSpeed;

            return this;
        }

        /**
         * Creates a DefaultMemoryModuleDevice object.
         *
         * @return DefaultMemoryModuleDevice object
         */
        public DefaultMemoryModuleDevice build() {
            return new DefaultMemoryModuleDevice(
                type, manufacturer, serialNumber,
                dataWidth, totalWidth, capacity,
                speed, configuredSpeed);
        }

    }

}
