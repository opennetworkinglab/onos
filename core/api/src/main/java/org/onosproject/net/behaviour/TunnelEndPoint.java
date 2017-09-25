/*
 * Copyright 2015-present Open Networking Foundation
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
 * Represents for source end point or destination end point of a tunnel. Maybe a tunnel
 * based on ConnectPoint, IpAddress, MacAddress and so on is built.
 */
public final class TunnelEndPoint<T> {

    private final T value;

    /**
     * Default constructor.
     *
     * @param value value of the tunnel endpoint
     */
    public TunnelEndPoint(T value) {
        this.value = value;
    }

    /**
     * Returns the value.
     *
     * @return tunnel endpoint value
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
        if (obj instanceof TunnelEndPoint) {
            final TunnelEndPoint that = (TunnelEndPoint) obj;
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
