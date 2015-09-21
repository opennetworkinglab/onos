/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgp.controller;

import org.onlab.packet.IpAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The class representing a network peer bgp ip.
 * This class is immutable.
 */
public final class BGPId {

    private static final String SCHEME = "bgp";
    private static final long UNKNOWN = 0;
    private final IpAddress ipAddress;

    /**
     * Private constructor.
     */
    private BGPId(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Create a BGPId from ip address.
     *
     * @param ipAddress IP address
     * @return object of BGPId
     */
    public static BGPId bgpId(IpAddress ipAddress) {
        return new BGPId(ipAddress);
    }

    /**
     * Returns the ip address.
     *
     * @return ipAddress
     */
    public IpAddress ipAddress() {
        return ipAddress;
    }

    /**
     * Convert the BGPId value to a ':' separated hexadecimal string.
     *
     * @return the BGPId value as a ':' separated hexadecimal string.
     */
    @Override
    public String toString() {
        return ipAddress.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BGPId)) {
            return false;
        }

        BGPId otherBGPid = (BGPId) other;
        return Objects.equals(ipAddress, otherBGPid.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress);
    }

    /**
     * Returns BGPId created from the given device URI.
     *
     * @param uri device URI
     * @return object of BGPId
     */
    public static BGPId bgpId(URI uri) {
        checkArgument(uri.getScheme().equals(SCHEME), "Unsupported URI scheme");
        return new BGPId(IpAddress.valueOf(uri.getSchemeSpecificPart()));
    }

    /**
     * Produces device URI from the given DPID.
     *
     * @param bgpId device bgpId
     * @return device URI
     */
    public static URI uri(BGPId bgpId) {
        return uri(bgpId.ipAddress());
    }

    /**
     * Produces device URI from the given DPID long.
     *
     * @param ipAddress device ip address
     * @return device URI
     */
    public static URI uri(IpAddress ipAddress) {
        try {
            return new URI(SCHEME, ipAddress.toString(), null);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}