/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.behaviour;

import java.util.Objects;

/**
 * Represent for a tunnel key. The tunnel accepts packets with the tunnel key.
 * A positive 24-bit (for Geneve, VXLAN, and LISP), 32-bit (for GRE) or 64-bit (for
 * GRE64) number value is used for example. Open vSwitch allows "flow" as the key
 * to set this value with matching in the flow table.
 */
public final class TunnelKey<T> {

    private final T value;

    /**
     * Default constructor.
     *
     * @param value value of the tunnel key
     */
    public TunnelKey(T value) {
        this.value = value;
    }

    /**
     * Returns the value.
     *
     * @return tunnel key value
     */
    public T value() {
        return value;
    }

    /**
     * Returns the value as a string.
     *
     * @return string value
     */
    public String strValue() {
        return value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TunnelKey) {
            final TunnelKey that = (TunnelKey) obj;
            return this.getClass() == that.getClass() &&
                    Objects.equals(this.value, that.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
