/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.onosproject.bgp.controller;

import static com.google.common.base.Preconditions.checkArgument;

import java.net.URI;
import java.net.URISyntaxException;

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class representing a network  bgp device id. This class is immutable.
 */
public final class BgpDpid {
    private static final Logger log = LoggerFactory.getLogger(BgpDpid.class);

    private static final String SCHEME = "bgp";
    private static final long UNKNOWN = 0;
    private StringBuilder stringBuilder;
    public static final int NODE_DESCRIPTOR_LOCAL = 1;
    public static final int NODE_DESCRIPTOR_REMOTE = 2;

    /**
     * Initialize bgp id to generate URI.
     *
     * @param linkNlri node Nlri.
     * @param nodeDescriptorType node descriptor type, local/remote
     */
    public BgpDpid(final BgpLinkLsNlriVer4 linkNlri, int nodeDescriptorType) {
        this.stringBuilder = new StringBuilder("bgpls://");

        if (linkNlri.getRouteDistinguisher() != null) {
            this.stringBuilder.append(linkNlri.getRouteDistinguisher().getRouteDistinguisher()).append(':');
        }

        try {
            this.stringBuilder.append(linkNlri.getProtocolId()).append(':').append(linkNlri.getIdentifier())
            .append('/');

            if (nodeDescriptorType == NODE_DESCRIPTOR_LOCAL) {
                add(linkNlri.localNodeDescriptors());
            } else if (nodeDescriptorType == NODE_DESCRIPTOR_REMOTE) {
                add(linkNlri.remoteNodeDescriptors());
            }
        } catch (BgpParseException e) {
            log.info("Exception BgpId string: " + e.toString());
        }

    }

    /**
     * Initialize bgp id to generate URI.
     *
     * @param nodeNlri node Nlri.
     */
    public BgpDpid(final BgpNodeLSNlriVer4 nodeNlri) {
        this.stringBuilder = new StringBuilder("bgpls://");

        if (nodeNlri.getRouteDistinguisher() != null) {
            this.stringBuilder.append(nodeNlri.getRouteDistinguisher().getRouteDistinguisher()).append(':');
        }

        try {

            this.stringBuilder.append(nodeNlri.getProtocolId()).append(':').append(nodeNlri.getIdentifier())
            .append('/');

            add(nodeNlri.getLocalNodeDescriptors());

        } catch (BgpParseException e) {
            log.info("Exception node string: " + e.toString());
        }
    }

    BgpDpid add(final Object value) {
        if (value != null) {
            this.stringBuilder.append('&').append('=').append(value.toString());
        }
        return this;
    }

    @Override
    public String toString() {
        return this.stringBuilder.toString();
    }

    /**
     * Produces bgp URI.
     *
     * @param value string to get URI
     * @return bgp URI, otherwise null
     */
    public static URI uri(String value) {
        try {
            return new URI(SCHEME, value, null);
        } catch (URISyntaxException e) {
            log.info("Exception BgpId URI: " + e.toString());
        }
        return null;
    }

    /**
     * Returns bgpDpid created from the given device URI.
     *
     * @param uri device URI
     * @return object of BgpDpid
     */
    public static BgpDpid bgpDpid(URI uri) {
        checkArgument(uri.getScheme().equals(SCHEME), "Unsupported URI scheme");

        // TODO: return BgpDpid generated from uri
        return null;
    }
}
