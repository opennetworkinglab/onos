/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.igp.controller;

import org.projectfloodlight.openflow.util.HexString;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.util.Tools.fromHex;
import static org.onlab.util.Tools.toHex;

/**
 * The class representing a network switch DPID.
 * This class is immutable.
 */
public final class IgpDpid {

    private static final String SCHEME = "of";
    private static final long UNKNOWN = 0;
    private final long value;

    /**
     * Default constructor.
     */
    public IgpDpid() {
        this.value = IgpDpid.UNKNOWN;
    }

    /**
     * Constructor from a long value.
     *
     * @param value the value to use.
     */
    public IgpDpid(long value) {
        this.value = value;
    }

    /**
     * Constructor from a string.
     *
     * @param value the value to use.
     */
    public IgpDpid(String value) {
        this.value = HexString.toLong(value);
    }

    /**
     * Get the value of the DPID.
     *
     * @return the value of the DPID.
     */
    public long value() {
        return value;
    }

    /**
     * Convert the DPID value to a ':' separated hexadecimal string.
     *
     * @return the DPID value as a ':' separated hexadecimal string.
     */
    @Override
    public String toString() {
        return HexString.toHexString(this.value);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof IgpDpid)) {
            return false;
        }

        IgpDpid otherDpid = (IgpDpid) other;

        return value == otherDpid.value;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash += 31 * hash + (int) (value ^ value >>> 32);
        return hash;
    }

    /**
     * Returns DPID created from the given device URI.
     *
     * @param uri device URI
     * @return dpid
     */
    public static IgpDpid dpid(URI uri) {
        checkArgument(uri.getScheme().equals(SCHEME), "Unsupported URI scheme");
        return new IgpDpid(fromHex(uri.getSchemeSpecificPart()));
    }

    /**
     * Produces device URI from the given DPID.
     *
     * @param dpid device dpid
     * @return device URI
     */
    public static URI uri(IgpDpid dpid) {
        return uri(dpid.value);
    }

    /**
     * Produces device URI from the given DPID long.
     *
     * @param value device dpid as long
     * @return device URI
     */
    public static URI uri(long value) {
        try {
            return new URI(SCHEME, toHex(value), null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

}
