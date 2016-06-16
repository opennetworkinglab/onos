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

package org.onosproject.bmv2.api.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Information of a port of a BMv2 device.
 */
@Beta
public final class Bmv2PortInfo {

    private final String ifaceName;
    private final int number;
    private final boolean isUp;

    /**
     * Creates a new port description.
     *
     * @param ifaceName the common name of the network interface
     * @param number    a port number
     * @param isUp      interface status
     */
    public Bmv2PortInfo(String ifaceName, int number, boolean isUp) {
        this.ifaceName = ifaceName;
        this.number = number;
        this.isUp = isUp;
    }

    /**
     * Returns the common name the network interface used by this port.
     *
     * @return a string value
     */
    public String ifaceName() {
        return ifaceName;
    }

    /**
     * Returns the number of this port.
     *
     * @return an integer value
     */
    public int number() {
        return number;
    }

    /**
     * Returns true if the port is up, false otherwise.
     *
     * @return a boolean value
     */
    public boolean isUp() {
        return isUp;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ifaceName, number, isUp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2PortInfo other = (Bmv2PortInfo) obj;
        return Objects.equal(this.ifaceName, other.ifaceName)
                && Objects.equal(this.number, other.number)
                && Objects.equal(this.isUp, other.isUp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("ifaceName", ifaceName)
                .add("number", number)
                .add("isUp", isUp)
                .toString();
    }
}
