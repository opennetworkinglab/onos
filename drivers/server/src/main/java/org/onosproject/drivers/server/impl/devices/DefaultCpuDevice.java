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

import org.onosproject.drivers.server.devices.cpu.CpuCoreId;
import org.onosproject.drivers.server.devices.cpu.CpuDevice;
import org.onosproject.drivers.server.devices.cpu.CpuVendor;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CORE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_CPU_FREQUENCY_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_SOCKET_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_VENDOR_NULL;

/**
 * Default implementation for CPU core devices.
 */
public final class DefaultCpuDevice implements CpuDevice {

    private final CpuCoreId id;
    private final CpuVendor vendor;
    private final int socket;
    private final long frequency;

    private DefaultCpuDevice(CpuCoreId id, CpuVendor vendor, int socket, long frequency) {
        checkNotNull(id, MSG_CPU_CORE_ID_NULL);
        checkNotNull(vendor, MSG_CPU_VENDOR_NULL);
        checkArgument((socket >= 0) && (socket < CpuCoreId.MAX_CPU_SOCKET_NB),
            MSG_CPU_SOCKET_NEGATIVE);
        checkArgument((frequency > 0) && (frequency <= CpuDevice.MAX_FREQUENCY_MHZ),
            MSG_CPU_FREQUENCY_NEGATIVE);

        this.id = id;
        this.vendor = vendor;
        this.socket = socket;
        this.frequency = frequency;
    }

    /**
     * Creates a builder for DefaultCpuDevice object.
     *
     * @return builder object for DefaultCpuDevice object
     */
    public static DefaultCpuDevice.Builder builder() {
        return new Builder();
    }

    @Override
    public CpuCoreId id() {
        return this.id;
    }

    @Override
    public CpuVendor vendor() {
        return this.vendor;
    }

    @Override
    public int socket() {
        return this.socket;
    }

    @Override
    public long frequency() {
        return this.frequency;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id",        id())
                .add("vendor",    vendor())
                .add("socket",    socket())
                .add("frequency", frequency())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CpuDevice)) {
            return false;
        }
        CpuDevice device = (CpuDevice) obj;
        return  this.id() ==  device.id() &&
                this.vendor() == device.vendor() &&
                this.socket() == device.socket() &&
                this.frequency() == device.frequency();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vendor, socket, frequency);
    }

    public static final class Builder {
        CpuCoreId id = null;
        CpuVendor vendor = null;
        int socket = -1;
        long frequency = -1;

        private Builder() {

        }

        /**
         * Sets the CPU core ID of this CPU.
         *
         * @param logicalCoreId logical CPU core ID
         * @param physicalCoreId physical CPU core ID
         * @return builder object
         */
        public Builder setCoreId(int logicalCoreId, int physicalCoreId) {
            this.id = new CpuCoreId(logicalCoreId, physicalCoreId);

            return this;
        }

        /**
         * Sets the CPU vendor of this CPU.
         *
         * @param vendorStr CPU vendor as a string
         * @return builder object
         */
        public Builder setVendor(String vendorStr) {
            if (!Strings.isNullOrEmpty(vendorStr)) {
                this.vendor = CpuVendor.getByName(vendorStr);
            }

            return this;
        }

        /**
         * Sets the CPU socket of this CPU.
         *
         * @param socket CPU socket
         * @return builder object
         */
        public Builder setSocket(int socket) {
            this.socket = socket;

            return this;
        }

        /**
         * Sets the frequency of this CPU.
         *
         * @param frequency CPU frequency
         * @return builder object
         */
        public Builder setFrequency(long frequency) {
            this.frequency = frequency;

            return this;
        }

        /**
         * Creates a DefaultCpuDevice object.
         *
         * @return DefaultCpuDevice object
         */
        public DefaultCpuDevice build() {
            return new DefaultCpuDevice(id, vendor, socket, frequency);
        }

    }

}
