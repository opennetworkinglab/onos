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

package org.onosproject.ospf.controller;

import org.onlab.packet.IpAddress;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents an OSPF router id.
 */
public class OspfRouterId {

    private static final String SCHEME = "l3";
    private static final long UNKNOWN = 0;
    private final IpAddress ipAddress;

    /**
     * Creates an instance of OSPF router id.
     *
     * @param ipAddress IP address of the router
     */
    public OspfRouterId(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Creates an instance from ip address.
     *
     * @param ipAddress IP address
     * @return OSPF router id instance
     */
    public static OspfRouterId ospfRouterId(IpAddress ipAddress) {
        return new OspfRouterId(ipAddress);
    }

    /**
     * Creates OSPF router id instance from the URI.
     *
     * @param uri device URI
     * @return OSPF router id instance
     */
    public static OspfRouterId ospfRouterId(URI uri) {
        checkArgument(uri.getScheme().equals(SCHEME), "Unsupported URI scheme");
        return new OspfRouterId(IpAddress.valueOf(uri.getSchemeSpecificPart()));
    }

    /**
     * Returns device URI from the given router id.
     *
     * @param ospfRouterId router id instance
     * @return device URI
     */
    public static URI uri(OspfRouterId ospfRouterId) {
        return uri(ospfRouterId.ipAddress());
    }

    /**
     * Returns device URI from the given IP address.
     *
     * @param ipAddress device IP address
     * @return device URI
     */
    public static URI uri(IpAddress ipAddress) {
        try {
            return new URI(SCHEME, ipAddress.toString(), null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Returns the IP address.
     *
     * @return IP address
     */
    public IpAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public String toString() {
        return ipAddress.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OspfRouterId)) {
            return false;
        }

        OspfRouterId otherOspfRouterId = (OspfRouterId) other;
        return Objects.equals(ipAddress, otherOspfRouterId.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress);
    }
}