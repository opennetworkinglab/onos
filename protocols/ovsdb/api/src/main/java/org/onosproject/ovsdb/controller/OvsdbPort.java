/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.ovsdb.controller;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * The class representing a ovsdb port. This class is immutable.
 */
public final class OvsdbPort {

    private final OvsdbPortNumber portNumber;
    private final OvsdbPortName portName;

    /**
     * Constructor from  OvsdbPortNumber portNumber, OvsdbPortName portName.
     *
     * @param portNumber the portNumber to use
     * @param portName the portName to use
     */
    public OvsdbPort(OvsdbPortNumber portNumber, OvsdbPortName portName) {
        checkNotNull(portNumber, "portNumber is not null");
        checkNotNull(portName, "portName is not null");
        this.portNumber = portNumber;
        this.portName = portName;
    }

    /**
     * Gets the port number of port.
     *
     * @return the port number of port
     */
    public OvsdbPortNumber portNumber() {
        return portNumber;
    }

    /**
     * Gets the port name of port.
     *
     * @return the port name of port
     */
    public OvsdbPortName portName() {
        return portName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(portNumber, portName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbPort) {
            final OvsdbPort otherOvsdbPort = (OvsdbPort) obj;
            return Objects.equals(this.portNumber, otherOvsdbPort.portNumber)
                    && Objects.equals(this.portName, otherOvsdbPort.portName);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("portNumber", String.valueOf(portNumber.value()))
                .add("portName", portName.value()).toString();
    }
}
