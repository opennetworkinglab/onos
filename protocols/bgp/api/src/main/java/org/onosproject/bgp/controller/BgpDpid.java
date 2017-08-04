/*
 * Copyright 2015-present Open Networking Foundation
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
import java.util.List;
import java.util.ListIterator;

import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.BgpLSIdentifierTlv;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.IsIsNonPseudonode;
import org.onosproject.bgpio.types.IsIsPseudonode;
import org.onosproject.bgpio.types.OspfNonPseudonode;
import org.onosproject.bgpio.types.OspfPseudonode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class representing a network bgp device id.
 */
public final class BgpDpid {
    private static final Logger log = LoggerFactory.getLogger(BgpDpid.class);

    private static final String SCHEME = "l3";
    private static final long UNKNOWN = 0;
    private StringBuilder stringBuilder;
    public static final int NODE_DESCRIPTOR_LOCAL = 1;
    public static final int NODE_DESCRIPTOR_REMOTE = 2;

    /**
     * Initialize BGP id to generate URI.
     *
     * @param linkNlri node NLRI.
     * @param nodeDescriptorType node descriptor type, local/remote
     */
    public BgpDpid(final BgpLinkLsNlriVer4 linkNlri, int nodeDescriptorType) {
        this.stringBuilder = new StringBuilder("");

        if (linkNlri.getRouteDistinguisher() != null) {
            this.stringBuilder.append("RD=").append(linkNlri.getRouteDistinguisher()
                                            .getRouteDistinguisher()).append(":");
        }
        this.stringBuilder.append(":ROUTINGUNIVERSE=").append(((BgpLinkLsNlriVer4) linkNlri).getIdentifier());

        if (nodeDescriptorType == NODE_DESCRIPTOR_LOCAL) {
            log.debug("Local node descriptor added");
            add(linkNlri.localNodeDescriptors());
        } else if (nodeDescriptorType == NODE_DESCRIPTOR_REMOTE) {
            log.debug("Remote node descriptor added");
            add(linkNlri.remoteNodeDescriptors());
        }
    }

    /*
     * Get iso node ID in specified string format.
     */
    public String isoNodeIdString(byte[] isoNodeId) {
        if (isoNodeId != null) {
            return String.format("%02x%02x.%02x%02x.%02x%02x", isoNodeId[0], isoNodeId[1],
                                 isoNodeId[2], isoNodeId[3],
                                 isoNodeId[4], isoNodeId[5]);
        }
        return null;
    }

    /**
     * Initialize BGP id to generate URI.
     *
     * @param nlri node NLRI.
     */
    public BgpDpid(final BgpNodeLSNlriVer4 nlri) {
        this.stringBuilder = new StringBuilder("");
        if (((BgpNodeLSNlriVer4) nlri).getRouteDistinguisher() != null) {
            this.stringBuilder.append("RD=")
                    .append(((BgpNodeLSNlriVer4) nlri).getRouteDistinguisher().getRouteDistinguisher()).append(":");
        }

        this.stringBuilder.append(":ROUTINGUNIVERSE=").append(((BgpNodeLSNlriVer4) nlri).getIdentifier());
        add(((BgpNodeLSNlriVer4) nlri).getLocalNodeDescriptors().getNodedescriptors());
        log.debug("BgpDpid :: add");
    }

    /**
     * Obtains instance of this class by appending stringBuilder with node descriptor value.
     *
     * @param value node descriptor
     * @return instance of this class
     */
    public BgpDpid add(final NodeDescriptors value) {
        log.debug("BgpDpid :: add function");
        if (value != null) {
            List<BgpValueType> subTlvs = value.getSubTlvs();
            ListIterator<BgpValueType> listIterator = subTlvs.listIterator();
            while (listIterator.hasNext()) {
                BgpValueType tlv = listIterator.next();
                if (tlv.getType() == AutonomousSystemTlv.TYPE) {
                    this.stringBuilder.append(":ASN=").append(((AutonomousSystemTlv) tlv).getAsNum());
                } else if (tlv.getType() == BgpLSIdentifierTlv.TYPE) {
                    this.stringBuilder.append(":DOMAINID=").append(((BgpLSIdentifierTlv) tlv).getBgpLsIdentifier());
                } else if (tlv.getType() == NodeDescriptors.IGP_ROUTERID_TYPE) {
                    if (tlv instanceof IsIsNonPseudonode) {
                        this.stringBuilder.append(":ISOID=").append(
                                isoNodeIdString(((IsIsNonPseudonode) tlv).getIsoNodeId()));
                    } else if (tlv instanceof IsIsPseudonode) {
                        IsIsPseudonode isisPseudonode = ((IsIsPseudonode) tlv);
                        this.stringBuilder.append(":ISOID=").append(
                                isoNodeIdString(((IsIsPseudonode) tlv).getIsoNodeId()));
                        this.stringBuilder.append(":PSN=").append(isisPseudonode.getPsnIdentifier());
                    } else if (tlv instanceof OspfNonPseudonode) {
                        this.stringBuilder.append(":RID=").append(((OspfNonPseudonode) tlv).getrouterID());
                    } else if (tlv instanceof OspfPseudonode) {
                        this.stringBuilder.append(":RID=").append(((OspfPseudonode) tlv).getrouterID());
                    }
                }
            }
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
            log.debug("Exception BgpId URI: " + e.toString());
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
