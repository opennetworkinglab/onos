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
package org.onosproject.pcep.controller;

import static com.google.common.base.Preconditions.checkArgument;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * The class representing a network client pc ip.
 * This class is immutable.
 */
public final class PccId {

    private static final String SCHEME = "pcep";
    private static final long UNKNOWN = 0;
    private final IpAddress ipAddress;

    /**
     * Private constructor.
     */
    private PccId(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Create a PccId from ip address.
     *
     * @param ipAddress IP address
     * @return ipAddress
     */
    public static PccId pccId(IpAddress ipAddress) {
        return new PccId(ipAddress);
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
     * Convert the PccId value to a ':' separated hexadecimal string.
     *
     * @return the PccId value as a ':' separated hexadecimal string.
     */
    @Override
    public String toString() {
        return ipAddress.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PccId)) {
            return false;
        }

        PccId otherPccid = (PccId) other;
        return Objects.equals(ipAddress, otherPccid.ipAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress);
    }

    /**
     * Returns PccId created from the given client URI.
     *
     * @param uri device URI
     * @return pccid
     */
    public static PccId pccid(URI uri) {
        checkArgument(uri.getScheme().equals(SCHEME), "Unsupported URI scheme");
        return new PccId(IpAddress.valueOf(uri.getSchemeSpecificPart()));
    }

    /**
     * Produces client URI from the given DPID.
     *
     * @param pccid client pccid
     * @return client URI
     */
    public static URI uri(PccId pccid) {
        return uri(pccid.ipAddress());
    }

    /**
     * Produces client URI from the given ip address.
     *
     * @param ipAddress ip of client
     * @return client URI
     */
    public static URI uri(IpAddress ipAddress) {
        try {
            return new URI(SCHEME, ipAddress.toString(), null);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
