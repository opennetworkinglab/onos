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
package org.onosproject.bgp.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpLocalRib;
import org.onosproject.bgpio.protocol.BgpLSNlri;

import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetailsLocalRib;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpPrefixLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.types.IPv4AddressTlv;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.LinkStateAttributes;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.IsIsNonPseudonode;
import org.onosproject.bgpio.types.LocalPref;
import org.onosproject.bgpio.types.Origin;
import org.onosproject.bgpio.types.attr.BgpAttrRouterIdV4;
import org.onosproject.bgpio.types.attr.BgpLinkAttrMaxLinkBandwidth;
import org.onosproject.bgpio.types.attr.BgpLinkAttrUnRsrvdLinkBandwidth;
import org.onosproject.bgpio.types.attr.BgpLinkAttrTeDefaultMetric;
import org.onosproject.bgpio.types.attr.BgpLinkAttrIgpMetric;
import org.onosproject.cli.AbstractShellCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import java.util.Arrays;



@Command(scope = "onos", name = "bgp-rib", description = "lists RIB configuration")
public class BgpLocalRibDisplay extends AbstractShellCommand {
    private static final Logger log = LoggerFactory.getLogger(BgpLocalRibDisplay.class);
    private static final String NODETREE = "nodes";
    private static final String LINKTREE = "links";
    private static final String PREFIXTREE = "prefix";
    private static final String VPNNODETREE = "vpnnodes";
    private static final String VPNLINKTREE = "vpnlinkS";
    private static final String VPNPREFIXTREE = "vpnprefix";
    protected Origin origin;
    protected LocalPref localPref;
    protected BgpAttrRouterIdV4 bgpAttrRouterIdV4;
    protected IsIsNonPseudonode isIsNonPseudonode;
    protected AutonomousSystemTlv autonomousSystemTlv;
    protected IPv4AddressTlv iPv4AddressTlv;
    protected BgpLinkAttrMaxLinkBandwidth bgpLinkAttrMaxLinkBandwidth;
    protected BgpLinkAttrUnRsrvdLinkBandwidth bgpLinkAttrUnRsrvdLinkBandwidth;
    protected BgpLinkAttrTeDefaultMetric bgpLinkAttrTeDefaultMetric;
    protected BgpLinkAttrIgpMetric bgpLinkAttrIgpMetric;
    protected PathAttrNlriDetailsLocalRib pathAttrNlriDetailsLocalRib;
    protected MpReachNlri mpReachNlri;
    protected PathAttrNlriDetails pathAttrNlriDetails;
    protected BgpNodeLSNlriVer4.ProtocolType protocolType;
    protected BgpController bgpController = get(BgpController.class);
    protected BgpLocalRib bgpLocalRib = bgpController.bgpLocalRib();
    Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib> nodeTreeMap = bgpLocalRib.nodeTree();
    Set<BgpNodeLSIdentifier> nodekeySet = nodeTreeMap.keySet();
    Map<BgpLinkLSIdentifier, PathAttrNlriDetailsLocalRib> linkTreeMap = bgpLocalRib.linkTree();
    Set<BgpLinkLSIdentifier> linkkeySet = linkTreeMap.keySet();
    @Argument(index = 0, name = "name",
            description = "nodetree" + "\n" + "linktree" + "\n" + "prefixtree" + "\n" + "vpnnodetree" + "\n" +
                    "vpnlinktree" + "\n" + "vpnprefixtree", required = true, multiValued = false)
    String name = null;
    @Argument(index = 1, name = "numberofentries",
            description = "numberofentries", required = false, multiValued = false)
    int numberofentries;
    @Argument(index = 2, name = "vpnId",
            description = "vpnId", required = false, multiValued = false)
    String vpnId = null;
    private int count = 0;

    @Override
    protected void execute() {
        switch (name) {
            case NODETREE:
                displayNodes();
                break;
            case LINKTREE:
                displayLinks();
                break;
            case PREFIXTREE:
                displayPrefix();
                break;
            case VPNNODETREE:
                displayVpnNodes();
                break;
            case VPNLINKTREE:
                displayVpnLinks();
                break;
            case VPNPREFIXTREE:
                displayVpnPrefix();
                break;
            default:
                System.out.print("Unknown Command");
                break;
        }

    }

    private void displayNodes() {
        try {
            int counter = 0;
            print("Total number of entries = %s", nodeTreeMap.size());
            for (BgpNodeLSIdentifier nodes : nodekeySet) {
                if (numberofentries > nodeTreeMap.size() || numberofentries < 0) {
                    System.out.print("Wrong Argument");
                    break;
                } else if (counter < numberofentries) {
                    pathAttrNlriDetailsLocalRib = nodeTreeMap.get(nodes);
                    displayNode();
                    counter++;
                } else if (counter == 0) {
                    pathAttrNlriDetailsLocalRib = nodeTreeMap.get(nodes);
                    displayNode();


                }

            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP nodes: {}", e.getMessage());
        }

    }


    private void displayLinks() {
        try {
            int counter = 0;
            print("Total Number of entries = %d", linkTreeMap.size());
            for (BgpLinkLSIdentifier links : linkkeySet) {
                if (numberofentries > linkTreeMap.size() || numberofentries < 0) {
                    System.out.print("Wrong Argument");
                    break;
                } else if (counter < numberofentries) {
                    pathAttrNlriDetailsLocalRib = linkTreeMap.get(links);
                    print("Total number of entries = %d", linkTreeMap.size());
                    displayLink();
                    counter++;
                } else if (counter == 0) {
                    pathAttrNlriDetailsLocalRib = linkTreeMap.get(links);
                    displayLink();
                }

            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP links: {}", e.getMessage());
        }
    }

    private void displayPrefix() {
        try {
            this.bgpController = get(BgpController.class);
            bgpLocalRib = bgpController.bgpLocalRib();
            Map<BgpPrefixLSIdentifier, PathAttrNlriDetailsLocalRib> prefixmap = bgpLocalRib.prefixTree();
            Set<BgpPrefixLSIdentifier> prefixkeySet = prefixmap.keySet();
            for (BgpPrefixLSIdentifier prefix : prefixkeySet) {
                pathAttrNlriDetailsLocalRib = prefixmap.get(prefix);
                pathAttrNlriDetails = pathAttrNlriDetailsLocalRib.localRibNlridetails();
                print("No of entries = %d", prefixmap.size());
                System.out.print(pathAttrNlriDetailsLocalRib.toString());

            }


        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP prefixes: {}", e.getMessage());

        }
    }

    private void displayVpnNodes() {
        try {
            this.bgpController = get(BgpController.class);
            bgpLocalRib = bgpController.bgpLocalRib();
            Map<RouteDistinguisher, Map<BgpNodeLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnNode =
                    bgpLocalRib.vpnNodeTree();
            Set<RouteDistinguisher> vpnNodekeySet = vpnNode.keySet();
            for (RouteDistinguisher vpnNodes : vpnNodekeySet) {
                boolean invalidProcess = true;
                if (vpnId != null && Integer.parseInt(vpnId.trim()) == vpnNodes.hashCode()) {
                    invalidProcess = false;
                    displayNodes();
                }
                if (invalidProcess) {
                    print("%s\n", "Id " + vpnId + "does not exist...!!!");
                }
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP nodes based on VPN : {}", e.getMessage());
        }

    }

    private void displayVpnLinks() {
        try {
            this.bgpController = get(BgpController.class);
            bgpLocalRib = bgpController.bgpLocalRib();
            Map<RouteDistinguisher, Map<BgpLinkLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnLink =
                    bgpLocalRib.vpnLinkTree();
            Set<RouteDistinguisher> vpnLinkkeySet = vpnLink.keySet();
            for (RouteDistinguisher vpnLinks : vpnLinkkeySet) {
                boolean invalidProcess = true;
                if (vpnId != null && Integer.parseInt(vpnId.trim()) == vpnLinks.hashCode()) {
                    invalidProcess = false;
                    displayLinks();
                }
                if (invalidProcess) {
                    print("%s\n", "Id " + vpnId + "does not exist...!!!");
                }
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP links based on VPN : {}", e.getMessage());
        }
    }

    private void displayVpnPrefix() {
        try {
            this.bgpController = get(BgpController.class);
            bgpLocalRib = bgpController.bgpLocalRib();
            Map<RouteDistinguisher, Map<BgpPrefixLSIdentifier, PathAttrNlriDetailsLocalRib>> vpnPrefix =
                    bgpLocalRib.vpnPrefixTree();
            Set<RouteDistinguisher> vpnPrefixkeySet = vpnPrefix.keySet();
            for (RouteDistinguisher vpnprefixId : vpnPrefixkeySet) {
                boolean invalidProcess = true;
                if (vpnId != null && Integer.parseInt(vpnId.trim()) == vpnprefixId.hashCode()) {
                    invalidProcess = false;
                    displayPrefix();
                }
                if (invalidProcess) {
                    print("%s\n", "Id " + vpnId + "does not exist...!!!");
                }
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP prefixes based on VPN : {}", e.getMessage());
        }
    }

    private void displayNode() {


        pathAttrNlriDetails = pathAttrNlriDetailsLocalRib.localRibNlridetails();
        List<BgpValueType> bgpValueTypeList = pathAttrNlriDetails.pathAttributes();
        protocolType = pathAttrNlriDetails.protocolID();
        Iterator<BgpValueType> itrBgpValueType = bgpValueTypeList.iterator();
        while (itrBgpValueType.hasNext()) {
            BgpValueType bgpValueType = itrBgpValueType.next();
            if (bgpValueType instanceof Origin) {
                origin = (Origin) bgpValueType;
            } else if (bgpValueType instanceof LocalPref) {
                localPref = (LocalPref) bgpValueType;
            } else if (bgpValueType instanceof LinkStateAttributes) {
                LinkStateAttributes linkStateAttributes = (LinkStateAttributes) bgpValueType;
                List linkStateAttribiuteList = linkStateAttributes.linkStateAttributes();
                Iterator<BgpValueType> linkStateAttribiteIterator = linkStateAttribiuteList.iterator();
                while (linkStateAttribiteIterator.hasNext()) {
                    BgpValueType bgpValueType1 = linkStateAttribiteIterator.next();
                    if (bgpValueType1 instanceof BgpAttrRouterIdV4) {
                        bgpAttrRouterIdV4 = (BgpAttrRouterIdV4) bgpValueType1;
                    }
                }
            } else if (bgpValueType instanceof MpReachNlri) {
                mpReachNlri = (MpReachNlri) bgpValueType;
                List<BgpLSNlri> bgpLSNlris = mpReachNlri.mpReachNlri();
                Iterator<BgpLSNlri> bgpLsnlrisIterator = bgpLSNlris.iterator();
                while (bgpLsnlrisIterator.hasNext()) {
                    BgpLSNlri bgpLSNlri = bgpLsnlrisIterator.next();
                    if (bgpLSNlri instanceof BgpNodeLSNlriVer4) {
                        BgpNodeLSNlriVer4 bgpNodeLSNlriVer4 = (BgpNodeLSNlriVer4) bgpLSNlri;
                        BgpNodeLSIdentifier bgpNodeLSIdentifier = bgpNodeLSNlriVer4.getLocalNodeDescriptors();
                        NodeDescriptors nodeDescriptors = bgpNodeLSIdentifier.getNodedescriptors();
                        List<BgpValueType> bgpvalueTypesList = nodeDescriptors.getSubTlvs();
                        Iterator<BgpValueType> bgpValueTypeIterator = bgpvalueTypesList.iterator();
                        while (bgpValueTypeIterator.hasNext()) {
                            BgpValueType valueType = bgpValueTypeIterator.next();
                            if (valueType instanceof IsIsNonPseudonode) {
                                isIsNonPseudonode = (IsIsNonPseudonode) valueType;

                            }
                        }
                    }
                }
            }
        }
        print("RibAsNumber = %s,PeerIdentifier = %s,RibIpAddress = %s,ProtocolType = %s,Origin = %s,LocalPref = %s," +
                        "RouterID = %s,IsoNodeID = %s,NextHop = %s", pathAttrNlriDetailsLocalRib.localRibAsNum(),
                pathAttrNlriDetailsLocalRib.localRibIdentifier(), pathAttrNlriDetailsLocalRib.localRibIpAddress(),
                protocolType.toString(), origin.origin(), localPref.localPref(), bgpAttrRouterIdV4.attrRouterId(),
                Arrays.toString(isIsNonPseudonode.getIsoNodeId()), mpReachNlri.nexthop4());
    }

    private void displayLink() {

        pathAttrNlriDetails = pathAttrNlriDetailsLocalRib.localRibNlridetails();
        List<BgpValueType> valueTypes = pathAttrNlriDetails.pathAttributes();
        Iterator<BgpValueType> itrBgpValueType = valueTypes.iterator();
        while (itrBgpValueType.hasNext()) {
            BgpValueType bgpValueType = itrBgpValueType.next();
            if (bgpValueType instanceof Origin) {
                origin = (Origin) bgpValueType;
            } else if (bgpValueType instanceof LocalPref) {
                localPref = (LocalPref) bgpValueType;
            } else if (bgpValueType instanceof LinkStateAttributes) {
                LinkStateAttributes linkStateAttributes = (LinkStateAttributes) bgpValueType;
                List linkStateAttributelist = linkStateAttributes.linkStateAttributes();
                Iterator<BgpValueType> linkStateAttributeIterator = linkStateAttributelist.iterator();
                while (linkStateAttributeIterator.hasNext()) {
                    BgpValueType bgpValueType1 = linkStateAttributeIterator.next();
                    if (bgpValueType1 instanceof BgpAttrRouterIdV4) {
                        bgpAttrRouterIdV4 = (BgpAttrRouterIdV4) bgpValueType1;
                    } else if (bgpValueType1 instanceof BgpLinkAttrMaxLinkBandwidth) {
                        bgpLinkAttrMaxLinkBandwidth = (BgpLinkAttrMaxLinkBandwidth) bgpValueType1;
                    } else if (bgpValueType1 instanceof BgpLinkAttrUnRsrvdLinkBandwidth) {
                        bgpLinkAttrUnRsrvdLinkBandwidth = (BgpLinkAttrUnRsrvdLinkBandwidth) bgpValueType1;
                    } else if (bgpValueType1 instanceof BgpLinkAttrTeDefaultMetric) {
                        bgpLinkAttrTeDefaultMetric = (BgpLinkAttrTeDefaultMetric) bgpValueType1;
                    } else if (bgpValueType1 instanceof BgpLinkAttrIgpMetric) {
                        bgpLinkAttrIgpMetric = (BgpLinkAttrIgpMetric) bgpValueType1;
                    }

                }
            } else if (bgpValueType instanceof MpReachNlri) {
                mpReachNlri = (MpReachNlri) bgpValueType;
                List<BgpLSNlri> bgpLSNlris = mpReachNlri.mpReachNlri();
                Iterator<BgpLSNlri> bgpLsnlrisIterator = bgpLSNlris.iterator();
                while (bgpLsnlrisIterator.hasNext()) {
                    BgpLSNlri bgpLSNlri = bgpLsnlrisIterator.next();
                    if (bgpLSNlri instanceof BgpLinkLsNlriVer4) {
                        BgpLinkLsNlriVer4 bgpLinkLsNlriVer4 = (BgpLinkLsNlriVer4) bgpLSNlri;
                        BgpLinkLSIdentifier bgpLinkLSIdentifier = bgpLinkLsNlriVer4.getLinkIdentifier();
                        NodeDescriptors localnodeDescriptors = bgpLinkLSIdentifier.localNodeDescriptors();
                        NodeDescriptors remotenodeDescriptors = bgpLinkLSIdentifier.remoteNodeDescriptors();
                        List<BgpValueType> linkDescriptors = bgpLinkLSIdentifier.linkDescriptors();
                        List<BgpValueType> subTlvList = localnodeDescriptors.getSubTlvs();
                        Iterator<BgpValueType> subTlvIterator = subTlvList.iterator();
                        while (subTlvIterator.hasNext()) {
                            BgpValueType valueType = subTlvIterator.next();
                            if (valueType instanceof IsIsNonPseudonode) {
                                isIsNonPseudonode = (IsIsNonPseudonode) valueType;
                            } else if (valueType instanceof AutonomousSystemTlv) {
                                autonomousSystemTlv = (AutonomousSystemTlv) valueType;
                            }
                        }
                        List<BgpValueType> remotevalueTypes = remotenodeDescriptors.getSubTlvs();
                        Iterator<BgpValueType> remoteValueTypesIterator = remotevalueTypes.iterator();
                        while (remoteValueTypesIterator.hasNext()) {
                            BgpValueType valueType = remoteValueTypesIterator.next();
                            if (valueType instanceof IsIsNonPseudonode) {
                                isIsNonPseudonode = (IsIsNonPseudonode) valueType;
                            } else if (valueType instanceof AutonomousSystemTlv) {
                                autonomousSystemTlv = (AutonomousSystemTlv) valueType;
                            }
                        }
                        Iterator<BgpValueType> listIterator = linkDescriptors.iterator();
                        while (listIterator.hasNext()) {
                            BgpValueType valueType = listIterator.next();
                            if (valueType instanceof IPv4AddressTlv) {
                                iPv4AddressTlv = (IPv4AddressTlv) valueType;
                            }

                        }
                    }
                }
            }
        }
        print("PeerIdentifier = %s,Origin = %s,LocalPref = %s,RouterID = %s,MaxBandwidth = %s," +
                        "UnreservedBandwidth = %s,DefaultMetric = %s,IGPMetric = %s,IsoNodeID = %s,ASNum = %s," +
                        "IPAddress = %s,NextHop = %s", pathAttrNlriDetailsLocalRib.localRibIdentifier(),
                origin.origin(), localPref.localPref(), bgpAttrRouterIdV4.attrRouterId(),
                bgpLinkAttrMaxLinkBandwidth.linkAttrMaxLinkBandwidth(),
                bgpLinkAttrUnRsrvdLinkBandwidth.getLinkAttrUnRsrvdLinkBandwidth().toString(),
                bgpLinkAttrTeDefaultMetric.attrLinkDefTeMetric(), bgpLinkAttrIgpMetric.attrLinkIgpMetric(),
                Arrays.toString(isIsNonPseudonode.getIsoNodeId()), autonomousSystemTlv.getAsNum(),
                iPv4AddressTlv.address(), mpReachNlri.nexthop4().toString(),
                pathAttrNlriDetailsLocalRib.localRibIpAddress(), origin.origin(), localPref.localPref(),
                bgpAttrRouterIdV4.attrRouterId());

    }
}