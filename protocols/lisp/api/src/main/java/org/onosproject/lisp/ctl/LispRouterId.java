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
package org.onosproject.lisp.ctl;

import org.onlab.packet.IpAddress;
import org.onlab.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * The class representing a network router identifier.
 * This class is immutable.
 */
public final class LispRouterId extends Identifier<IpAddress> {

    private static final Logger log = LoggerFactory.getLogger(LispRouterId.class);
    private static final String SCHEME = "lisp";
    private static final IpAddress UNKNOWN = IpAddress.valueOf("0.0.0.0");

    /**
     * A default constructor.
     */
    public LispRouterId() {
        super(UNKNOWN);
    }

    /**
     * A constructor with an IpAddress value specified.
     *
     * @param value the value to use
     */
    public LispRouterId(IpAddress value) {
        super(value);
    }

    /**
     * A constructor with a String value specified.
     *
     * @param value the value to use
     */
    public LispRouterId(String value) {
        super(IpAddress.valueOf(value));
    }

    /**
     * Returns LispRouterId created from a given device URI.
     *
     * @param uri device URI
     * @return object of LispRouterId
     */
    public static LispRouterId routerId(URI uri) {
        checkArgument(uri.getScheme().equals(SCHEME), "Unsupported URI scheme");
        return new LispRouterId(IpAddress.valueOf(uri.getSchemeSpecificPart()));
    }

    /**
     * Produces a device URI from the given LispRouterId.
     *
     * @param routerId device identifier
     * @return device URI
     */
    public static URI uri(LispRouterId routerId) {
        return uri(routerId.id());
    }

    /**
     * Produces device URI from the given device IpAddress.
     *
     * @param ipAddress device ip address
     * @return device URI
     */
    public static URI uri(IpAddress ipAddress) {
        try {
            return new URI(SCHEME, ipAddress.toString(), null);
        } catch (URISyntaxException e) {
            log.warn("Failed to parse the IP address.", e);
            return null;
        }
    }
}
