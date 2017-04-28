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

import static org.onosproject.net.PortNumber.P0;

/**
 * Representation of a network edge location where an end-station host is
 * connected.
 */
public class HostLocation extends ConnectPoint {

    /**
     * Represents a no location or an unknown location.
     */
    public static final HostLocation NONE = new HostLocation(DeviceId.NONE, P0, 0L);

    // Note that time is explicitly excluded from the notion of equality.
    private final long time;

    /**
     * Creates a new host location using the supplied device &amp; port.
     *
     * @param deviceId   device identity
     * @param portNumber device port number
     * @param time       time when detected, in millis since start of epoch
     */
    public HostLocation(DeviceId deviceId, PortNumber portNumber, long time) {
        super(deviceId, portNumber);
        this.time = time;
    }

    /**
     * Creates a new host location derived from the supplied connection point.
     *
     * @param connectPoint connection point
     * @param time         time when detected, in millis since start of epoch
     */
    public HostLocation(ConnectPoint connectPoint, long time) {
        super(connectPoint.deviceId(), connectPoint.port());
        this.time = time;
    }

    /**
     * Returns the time when the location was established, given in
     * milliseconds since start of epoch.
     *
     * @return time in milliseconds since start of epoch
     */
    public long time() {
        return time;
    }

    @Override
    public String toString() {
        return deviceId() + "/" + port();
    }
}
