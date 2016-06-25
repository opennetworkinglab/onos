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
package org.onosproject.pcep.api;

import java.net.URI;
import java.net.URISyntaxException;

import org.onosproject.pcep.tools.PcepTools;

/**
 * The class representing a network switch PCEPDid. This class is immutable.
 */
public final class PcepDpid {

    private static final String SCHEME = "l3";
    private static final long UNKNOWN = 0;
    private long nodeId;

    /**
     * Default constructor.
     */
    public PcepDpid() {
        this.nodeId = PcepDpid.UNKNOWN;
    }

    /**
     * Constructor from a long value.
     *
     * @param value long value for construct
     */
    public PcepDpid(long value) {
        this.nodeId = value;
    }

    /**
     * Constructor from a String.
     *
     * @param value string value for construct
     */
    public PcepDpid(String value) {
        this.nodeId = Long.parseLong(value, 16);
    }

    /**
     * Produces device URI from the given DPID.
     *
     * @param dpid device dpid
     * @return device URI
     */
    public static URI uri(PcepDpid dpid) {
        return uri(dpid.nodeId);
    }

    /**
     * Produces device long from the given string which comes from the uri
     * method.
     *
     * @param value string value which produced by uri method.
     * @return a long value.
     */
    public static long toLong(String value) {
        return PcepTools.ipToLong(value.replace(SCHEME, ""));
    }

    /**
     * Produces device URI from the given DPID long.
     *
     * @param value device dpid as long
     * @return device URI
     */
    public static URI uri(long value) {
        try {
            return new URI(SCHEME, PcepTools.longToIp(value), null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Return a device id with the form of long.
     *
     * @return long value
     */
    public long value() {
        return this.nodeId;
    }

}
