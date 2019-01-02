/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.segmentrouting.xconnect.api;

import org.onosproject.net.PortNumber;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a cross connect endpoint specified by port number.
 */
public final class XconnectPortEndpoint extends XconnectEndpoint {
    private final PortNumber port;

    private XconnectPortEndpoint(PortNumber port) {
        this.port = port;
    }

    /**
     * Returns port number.
     *
     * @return port number
     */
    public PortNumber port() {
        return port;
    }

    /**
     * Returns an instance of XconnectPortEndpoint with given port number.
     *
     * @param port port number
     * @return an instance of XconnectPortEndpoint
     */
    public static XconnectPortEndpoint of(PortNumber port) {
        return new XconnectPortEndpoint(port);
    }

    /**
     * Gets XconnectPortEndpoint from string.
     *
     * @param s string
     * @return XconnectPortEndpoint
     */
    public static XconnectPortEndpoint fromString(String s) {
        checkArgument(s.matches(PORT_PATTERN), "String {} does not match {} format", s, PORT_PATTERN);
        return new XconnectPortEndpoint(PortNumber.fromString(s));
    }

    @Override
    public XconnectEndpoint.Type type() {
        return Type.PORT;
    }

    @Override
    public int hashCode() {
        return Objects.hash(port);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof XconnectPortEndpoint) {
            final XconnectPortEndpoint other = (XconnectPortEndpoint) obj;
            return Objects.equals(this.port, other.port);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(port);
    }
}
