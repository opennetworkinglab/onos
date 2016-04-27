/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.provider.bgp.topology.impl;

import static org.onosproject.bgp.controller.BgpDpid.uri;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Device.Type.ROUTER;
import static org.onosproject.net.Device.Type.VIRTUAL;

import java.util.List;
import java.util.Set;

import org.onlab.packet.ChassisId;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpDpid;
import org.onosproject.bgp.controller.BgpLinkListener;
import org.onosproject.bgp.controller.BgpNodeListener;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpLinkLsNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSIdentifier;
import org.onosproject.bgpio.protocol.linkstate.BgpNodeLSNlriVer4;
import org.onosproject.bgpio.protocol.linkstate.NodeDescriptors;
import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetails;
import org.onosproject.bgpio.types.AutonomousSystemTlv;
import org.onosproject.bgpio.types.BgpLSIdentifierTlv;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.IPv4AddressTlv;
import org.onosproject.bgpio.types.IsIsNonPseudonode;
import org.onosproject.bgpio.types.IsIsPseudonode;
import org.onosproject.bgpio.types.LinkLocalRemoteIdentifiersTlv;
import org.onosproject.bgpio.types.OspfNonPseudonode;
import org.onosproject.bgpio.types.OspfPseudonode;
import org.onosproject.core.CoreService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider which uses an BGP controller to detect network infrastructure topology.
 */
@Component(immediate = true)
public class BgpTopologyProvider extends AbstractProvider implements DeviceProvider, LinkProvider {

    /**
     * Creates an instance of BGP topology provider.
     */
    public BgpTopologyProvider() {
        super(new ProviderId("l3", "org.onosproject.provider.bgp"));
    }

    private static final Logger log = LoggerFactory.getLogger(BgpTopologyProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BgpController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private DeviceProviderService deviceProviderService;
    private LinkProviderService linkProviderService;

    private InternalBgpProvider listener = new InternalBgpProvider();
    private static final String UNKNOWN = "unknown";
    public static final long IDENTIFIER_SET = 0x100000000L;
    public static final String AS_NUMBER = "asNumber";
    public static final String DOMAIN_IDENTIFIER = "domainIdentifier";
    public static final String ROUTING_UNIVERSE = "routingUniverse";
    public static final long PSEUDO_PORT = 0xffffffff;

    @Activate
    public void activate() {
        log.debug("BgpTopologyProvider activate");
        deviceProviderService = deviceProviderRegistry.register(this);
        linkProviderService = linkProviderRegistry.register(this);
        controller.addListener(listener);
        controller.addLinkListener(listener);
    }

    @Deactivate
    public void deactivate() {
        log.debug("BgpTopologyProvider deactivate");
        deviceProviderRegistry.unregister(this);
        deviceProviderService = null;
        linkProviderRegistry.unregister(this);
        linkProviderService = null;
        controller.removeListener(listener);
        controller.removeLinkListener(listener);
    }

    /*
     * Implements device and link update.
     */
    private class InternalBgpProvider implements BgpNodeListener, BgpLinkListener {

        @Override
        public void addNode(BgpNodeLSNlriVer4 nodeNlri, PathAttrNlriDetails details) {
            log.debug("Add node {}", nodeNlri.toString());

            if (deviceProviderService == null) {
                return;
            }
            Device.Type deviceType = ROUTER;
            BgpDpid nodeUri = new BgpDpid(nodeNlri);
            DeviceId deviceId = deviceId(uri(nodeUri.toString()));
            ChassisId cId = new ChassisId();

            DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();

            newBuilder.set(AnnotationKeys.TYPE, "L3");
            newBuilder.set(ROUTING_UNIVERSE, Long.toString(nodeNlri.getIdentifier()));

            List<BgpValueType> tlvs = nodeNlri.getLocalNodeDescriptors().getNodedescriptors().getSubTlvs();
            for (BgpValueType tlv : tlvs) {
                if (tlv instanceof AutonomousSystemTlv) {
                    newBuilder.set(AS_NUMBER, Integer.toString(((AutonomousSystemTlv) tlv).getAsNum()));
                } else if (tlv instanceof BgpLSIdentifierTlv) {
                    newBuilder.set(DOMAIN_IDENTIFIER,
                            Integer.toString(((BgpLSIdentifierTlv) tlv).getBgpLsIdentifier()));
                }
                if (tlv.getType() == NodeDescriptors.IGP_ROUTERID_TYPE) {
                    if (tlv instanceof IsIsPseudonode) {
                        deviceType = VIRTUAL;
                        newBuilder.set(AnnotationKeys.ROUTER_ID, new String(((IsIsPseudonode) tlv).getIsoNodeId()));
                    } else if (tlv instanceof OspfPseudonode) {
                        deviceType = VIRTUAL;
                        newBuilder
                                .set(AnnotationKeys.ROUTER_ID, Integer.toString(((OspfPseudonode) tlv).getrouterID()));
                    } else if (tlv instanceof IsIsNonPseudonode) {
                        newBuilder.set(AnnotationKeys.ROUTER_ID, new String(((IsIsNonPseudonode) tlv).getIsoNodeId()));
                    } else if (tlv instanceof OspfNonPseudonode) {
                        newBuilder.set(AnnotationKeys.ROUTER_ID,
                                Integer.toString(((OspfNonPseudonode) tlv).getrouterID()));
                    }
                }
            }

            DeviceDescription description = new DefaultDeviceDescription(uri(nodeUri.toString()), deviceType, UNKNOWN,
                    UNKNOWN, UNKNOWN, UNKNOWN, cId, newBuilder.build());
            deviceProviderService.deviceConnected(deviceId, description);

        }

        @Override
        public void deleteNode(BgpNodeLSNlriVer4 nodeNlri) {
            log.debug("Delete node {}", nodeNlri.toString());

            if (deviceProviderService == null) {
                return;
            }

            BgpDpid deviceUri = new BgpDpid(nodeNlri);
            DeviceId deviceId = deviceId(uri(deviceUri.toString()));
            deviceProviderService.deviceDisconnected(deviceId);
        }

        @Override
        public void addLink(BgpLinkLsNlriVer4 linkNlri, PathAttrNlriDetails details) throws BgpParseException {
            log.debug("Addlink {}", linkNlri.toString());

            if (linkProviderService == null) {
                return;
            }
            LinkDescription linkDes = buildLinkDes(linkNlri, details, true);
            linkProviderService.linkDetected(linkDes);
        }

        //Build link description.
        private LinkDescription buildLinkDes(BgpLinkLsNlriVer4 linkNlri, PathAttrNlriDetails details, boolean isAddLink)
                throws BgpParseException {
            long srcAddress = 0;
            long dstAddress = 0;
            boolean localPseduo = false;
            boolean remotePseduo = false;

            List<BgpValueType> localTlvs = linkNlri.getLinkIdentifier().localNodeDescriptors().getSubTlvs();
            for (BgpValueType localTlv : localTlvs) {
                if (localTlv instanceof IsIsPseudonode || localTlv instanceof OspfPseudonode) {
                    localPseduo = true;
                }
            }
            List<BgpValueType> remoteTlvs = linkNlri.getLinkIdentifier().remoteNodeDescriptors().getSubTlvs();
            for (BgpValueType remoteTlv : remoteTlvs) {
                if (remoteTlv instanceof IsIsPseudonode || remoteTlv instanceof OspfPseudonode) {
                    remotePseduo = true;
                }
            }

            List<BgpValueType> tlvs = linkNlri.getLinkIdentifier().linkDescriptors();
            for (BgpValueType tlv : tlvs) {
                if (tlv instanceof LinkLocalRemoteIdentifiersTlv) {
                    srcAddress = ((LinkLocalRemoteIdentifiersTlv) tlv).getLinkLocalIdentifier();
                    //Set 32nd bit.
                    srcAddress = srcAddress | IDENTIFIER_SET;
                    dstAddress = ((LinkLocalRemoteIdentifiersTlv) tlv).getLinkRemoteIdentifier();
                    dstAddress = dstAddress | IDENTIFIER_SET;
                } else if (tlv instanceof IPv4AddressTlv) {
                    if (tlv.getType() == BgpLinkLSIdentifier.IPV4_INTERFACE_ADDRESS_TYPE) {
                        srcAddress = ((IPv4AddressTlv) tlv).address().toInt();
                    } else {
                        dstAddress = ((IPv4AddressTlv) tlv).address().toInt();
                    }
                }
            }

            DeviceId srcId = deviceId(uri(new BgpDpid(linkNlri, BgpDpid.NODE_DESCRIPTOR_LOCAL).toString()));
            DeviceId dstId = deviceId(uri(new BgpDpid(linkNlri, BgpDpid.NODE_DESCRIPTOR_REMOTE).toString()));

            if (localPseduo && srcAddress == 0) {
                srcAddress = PSEUDO_PORT;
            } else if (remotePseduo && dstAddress == 0) {
                dstAddress = PSEUDO_PORT;
            }

            ConnectPoint src = new ConnectPoint(srcId, PortNumber.portNumber(srcAddress));
            ConnectPoint dst = new ConnectPoint(dstId, PortNumber.portNumber(dstAddress));
            BgpNodeLSNlriVer4 srcNodeNlri = new BgpNodeLSNlriVer4(linkNlri.getIdentifier(), linkNlri.getProtocolId()
                    .getType(), new BgpNodeLSIdentifier(linkNlri.getLinkIdentifier().localNodeDescriptors()), false,
                    linkNlri.getRouteDistinguisher());

            BgpNodeLSNlriVer4 dstNodeNlri = new BgpNodeLSNlriVer4(linkNlri.getIdentifier(), linkNlri.getProtocolId()
                    .getType(), new BgpNodeLSIdentifier(linkNlri.getLinkIdentifier().remoteNodeDescriptors()), false,
                    linkNlri.getRouteDistinguisher());

            addOrDeletePseudoNode(isAddLink, localPseduo, remotePseduo, srcNodeNlri,
                     dstNodeNlri, srcId, dstId, details);
            return new DefaultLinkDescription(src, dst, Link.Type.DIRECT, false);
        }

        private void addOrDeletePseudoNode(boolean isAddLink, boolean localPseduo, boolean remotePseduo,
                BgpNodeLSNlriVer4 srcNodeNlri, BgpNodeLSNlriVer4 dstNodeNlri, DeviceId srcId, DeviceId dstId,
                PathAttrNlriDetails details) {
            if (isAddLink) {
                if (localPseduo) {
                    if (deviceService.getDevice(srcId) == null) {
                        for (BgpNodeListener l : controller.listener()) {
                            l.addNode(srcNodeNlri, details);
                        }
                    }
                } else if (remotePseduo) {
                    if (deviceService.getDevice(dstId) == null) {
                        for (BgpNodeListener l : controller.listener()) {
                            l.addNode(dstNodeNlri, details);
                        }
                    }
                }
            } else {
                if (localPseduo) {
                    Set<Link> links = linkService.getDeviceLinks(srcId);
                    if (links == null || links.isEmpty()) {
                        for (BgpNodeListener l : controller.listener()) {
                            l.deleteNode(srcNodeNlri);
                        }
                    }
                } else if (remotePseduo) {
                    log.info("Remote pseudo delete link ");
                    Set<Link> links = linkService.getDeviceLinks(dstId);
                    if (links == null || links.isEmpty()) {
                        for (BgpNodeListener l : controller.listener()) {
                            l.deleteNode(dstNodeNlri);
                        }
                    }
                }
            }
        }

        @Override
        public void deleteLink(BgpLinkLsNlriVer4 linkNlri) throws BgpParseException {
            log.debug("Delete link {}", linkNlri.toString());

            if (linkProviderService == null) {
                return;
            }

            LinkDescription linkDes = buildLinkDes(linkNlri, null, false);
            linkProviderService.linkVanished(linkDes);
        }
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        // TODO Auto-generated method stub
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
    }
}
