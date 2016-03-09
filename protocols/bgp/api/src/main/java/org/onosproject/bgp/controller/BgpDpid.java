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
import java.util.List;
import java.util.ListIterator;

import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.types.AreaIDTlv;
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
        this.stringBuilder = new StringBuilder("");

        if (linkNlri.getRouteDistinguisher() != null) {
            this.stringBuilder.append("RD=").append(linkNlri.getRouteDistinguisher()
                                            .getRouteDistinguisher()).append(":");
        }

        try {
            if ((linkNlri.getProtocolId() == BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_ONE)
                || (linkNlri.getProtocolId() == BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO)) {
                this.stringBuilder.append("PROTO=").append("ISIS").append(":ID=")
                                                                               .append(linkNlri.getIdentifier());
            } else {
                this.stringBuilder.append("PROTO=").append(linkNlri.getProtocolId()).append(":ID=")
                                                                               .append(linkNlri.getIdentifier());
            }

            if (nodeDescriptorType == NODE_DESCRIPTOR_LOCAL) {
                add(linkNlri.localNodeDescriptors());
            } else if (nodeDescriptorType == NODE_DESCRIPTOR_REMOTE) {
                add(linkNlri.remoteNodeDescriptors());
            }
        } catch (BgpParseException e) {
            log.info("Exception BgpId string: " + e.toString());
        }

    }

    /*
     * Get iso node ID in specified string format.
     */
    private String isoNodeIdString(byte[] isoNodeId) {
        if (isoNodeId != null) {
            return String.format("%02x%02x.%02x%02x.%02x%02x", isoNodeId[0], isoNodeId[1],
                                 isoNodeId[2], isoNodeId[3],
                                 isoNodeId[4], isoNodeId[5]);
        }
        return null;
    }

    /**
     * Initialize bgp id to generate URI.
     *
     * @param nodeNlri node Nlri.
     */
    public BgpDpid(final BgpNodeLSNlriVer4 nodeNlri) {
        this.stringBuilder = new StringBuilder("");

        if (nodeNlri.getRouteDistinguisher() != null) {
            this.stringBuilder.append("RD=").append(nodeNlri.getRouteDistinguisher()
                                            .getRouteDistinguisher()).append(":");
        }

        try {
            if ((nodeNlri.getProtocolId() == BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_ONE)
                || (nodeNlri.getProtocolId() == BgpNodeLSNlriVer4.ProtocolType.ISIS_LEVEL_TWO)) {

                this.stringBuilder.append("PROTO=").append("ISIS").append(":ID=")
                                                                               .append(nodeNlri.getIdentifier());
            } else {
                this.stringBuilder.append("PROTO=").append(nodeNlri.getProtocolId()).append(":ID=")
                                                                               .append(nodeNlri.getIdentifier());
            }
            add(nodeNlri.getLocalNodeDescriptors());

        } catch (BgpParseException e) {
            log.info("Exception node string: " + e.toString());
        }
    }

    BgpDpid add(final Object value) {
      NodeDescriptors nodeDescriptors = null;
        if (value instanceof  BgpNodeLSIdentifier) {
            BgpNodeLSIdentifier nodeLsIdentifier = (BgpNodeLSIdentifier) value;
            nodeDescriptors = nodeLsIdentifier.getNodedescriptors();
        } else if (value instanceof  NodeDescriptors) {
            nodeDescriptors = (NodeDescriptors) value;
        }

        if (nodeDescriptors != null) {
            List<BgpValueType> subTlvs = nodeDescriptors.getSubTlvs();
            ListIterator<BgpValueType> listIterator = subTlvs.listIterator();
            while (listIterator.hasNext()) {
                BgpValueType tlv = listIterator.next();
                if (tlv.getType() == AutonomousSystemTlv.TYPE) {
                    AutonomousSystemTlv autonomousSystem = (AutonomousSystemTlv) tlv;
                    this.stringBuilder.append(":AS=").append(autonomousSystem.getAsNum());
                } else if (tlv.getType() == BgpLSIdentifierTlv.TYPE) {
                    BgpLSIdentifierTlv lsIdentifierTlv = (BgpLSIdentifierTlv) tlv;
                    this.stringBuilder.append(":LSID=").append(lsIdentifierTlv.getBgpLsIdentifier());
                } else if (tlv.getType() ==  AreaIDTlv.TYPE) {
                    AreaIDTlv areaIdTlv = (AreaIDTlv) tlv;
                    this.stringBuilder.append(":AREA=").append(areaIdTlv.getAreaID());
                } else if (tlv.getType() == NodeDescriptors.IGP_ROUTERID_TYPE) {
                    if (tlv instanceof IsIsNonPseudonode) {
                        IsIsNonPseudonode isisNonPseudonode = (IsIsNonPseudonode) tlv;
                        this.stringBuilder.append(":ISOID=").append(isoNodeIdString(isisNonPseudonode.getIsoNodeId()));
                    } else if (tlv instanceof IsIsPseudonode) {
                        IsIsPseudonode isisPseudonode = (IsIsPseudonode) tlv;
                        this.stringBuilder.append(":ISOID=").append(isoNodeIdString(isisPseudonode.getIsoNodeId()));
                        this.stringBuilder.append(":PSN=").append(isisPseudonode.getPsnIdentifier());
                    } else if (tlv instanceof OspfNonPseudonode) {
                        OspfNonPseudonode ospfNonPseudonode = (OspfNonPseudonode) tlv;
                        this.stringBuilder.append(":RID=").append(ospfNonPseudonode.getrouterID());
                    } else if (tlv instanceof OspfPseudonode) {
                        OspfPseudonode ospfPseudonode = (OspfPseudonode) tlv;
                        this.stringBuilder.append(":RID=").append(ospfPseudonode.getrouterID());
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
