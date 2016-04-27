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

package org.onosproject.bgp.controller;

import org.onlab.packet.IpAddress;
import org.onlab.util.Identifier;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The class representing a network peer bgp ip.
 * This class is immutable.
 */
public final class BgpId extends Identifier<IpAddress> {

    private static final String SCHEME = "l3";
    private static final long UNKNOWN = 0;

    /**
     * Constructor to initialize ipAddress.
     *
     * @param ipAddress Ip address
     */
    public BgpId(IpAddress ipAddress) {
        super(ipAddress);
    }

    /**
     * Create a BGPId from ip address.
     *
     * @param ipAddress IP address
     * @return object of BGPId
     */
    public static BgpId bgpId(IpAddress ipAddress) {
        return new BgpId(ipAddress);
    }

    /**
     * Returns the ip address.
     *
     * @return ipAddress
     */
    public IpAddress ipAddress() {
        return identifier;
    }

    /**
     * Returns BGPId created from the given device URI.
     *
     * @param uri device URI
     * @return object of BGPId
     */
    public static BgpId bgpId(URI uri) {
        checkArgument(uri.getScheme().equals(SCHEME), "Unsupported URI scheme");
        return new BgpId(IpAddress.valueOf(uri.getSchemeSpecificPart()));
    }

    /**
     * Produces device URI from the given DPID.
     *
     * @param bgpId device bgpId
     * @return device URI
     */
    public static URI uri(BgpId bgpId) {
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
