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

package org.onosproject.isis.controller.topology;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents an ISIS router id.
 */
public class IsisRouterId {

    private static final String SCHEME = "l3";
    private static final long UNKNOWN = 0;
    private final String ipAddress;

    /**
     * Creates an instance of ISIS router id.
     *
     * @param ipAddress IP address of the router
     */
    public IsisRouterId(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Creates an instance from ip address.
     *
     * @param ipAddress IP address
     * @return ISIS router id instance
     */
    public static IsisRouterId isisRouterId(String ipAddress) {
        return new IsisRouterId(ipAddress);
    }

    /**
     * Creates ISIS router id instance from the URI.
     *
     * @param uri device URI
     * @return ISIS router id instance
     */
    public static IsisRouterId isisRouterId(URI uri) {
        checkArgument(uri.getScheme().equals(SCHEME), "Unsupported URI scheme");
        return new IsisRouterId(uri.getSchemeSpecificPart());
    }

    /**
     * Returns device URI from the given router id.
     *
     * @param isisRouterId router id instance
     * @return device URI
     */
    public static URI uri(IsisRouterId isisRouterId) {
        return uri(isisRouterId.ipAddress());
    }

    /**
     * Returns device URI from the given IP address.
     *
     * @param ipAddress device IP address
     * @return device URI
     */
    public static URI uri(String ipAddress) {
        try {
            return new URI(SCHEME, ipAddress, null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Returns the IP address.
     *
     * @return IP address
     */
    public String ipAddress() {
        return ipAddress;
    }

    @Override
    public String toString() {
        return ipAddress;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IsisRouterId)) {
            return false;
        }

        IsisRouterId otherIsisRouterId = (IsisRouterId) other;
        return Objects.equals(ipAddress, otherIsisRouterId.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress);
    }
}