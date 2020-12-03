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
package org.onosproject.net.flow.criteria;

import org.onosproject.net.PortNumber;

import java.util.Objects;

/**
 * Implementation of input port criterion.
 */
public final class PortCriterion implements Criterion {
    private final PortNumber port;
    private final Type type;

    /**
     * Constructor.
     *
     * @param port the input port number to match
     * @param type the match type. Should be either Type.IN_PORT or
     * Type.IN_PHY_PORT
     */
    PortCriterion(PortNumber port, Type type) {
        this.port = port;
        this.type = type;
    }

    @Override
    public Type type() {
        return this.type;
    }

    /**
     * Gets the input port number to match.
     *
     * @return the input port number to match
     */
    public PortNumber port() {
        return this.port;
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + port.toStringWithoutName();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PortCriterion) {
            PortCriterion that = (PortCriterion) obj;
            return Objects.equals(port, that.port) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
