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

package org.onosproject.drivers.server.devices.nic;

import java.util.Objects;

/**
 * The base class that holds the value of a NIC's Rx filter.
 */
public abstract class RxFilterValue {

    /**
     * CPU ID of the server this tag will lead to.
     */
    protected int cpuId;

    /**
     * Constructs an Rx filter value.
     *
     * @param cpuId CPU ID of the server this tag will lead to
     */
    public RxFilterValue(int cpuId) {
        this.cpuId = cpuId;
    }

    /**
     * Returns the CPU ID that corresponds to this Rx filter value.
     *
     * @return CPU ID of the server this tag will lead to
     */
    public int cpuId() {
        return this.cpuId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.cpuId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || (!(obj instanceof RxFilterValue))) {
            return false;
        }

        return cpuId == ((RxFilterValue) obj).cpuId;
    }

}
