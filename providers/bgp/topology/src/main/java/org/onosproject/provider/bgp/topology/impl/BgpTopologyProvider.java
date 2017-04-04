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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;
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
import org.onosproject.bgpio.types.LinkStateAttributes;
import org.onosproject.bgpio.types.OspfNonPseudonode;
import org.onosproject.bgpio.types.OspfPseudonode;
import org.onosproject.bgpio.types.attr.BgpAttrNodeFlagBitTlv;
import org.onosproject.bgpio.types.attr.BgpAttrNodeIsIsAreaId;
import org.onosproject.bgpio.types.attr.BgpAttrRouterIdV4;
import org.onosproject.bgpio.types.attr.BgpLinkAttrIgpMetric;
import org.onosproject.bgpio.types.attr.BgpLinkAttrMaxLinkBandwidth;
import org.onosproject.bgpio.types.attr.BgpLinkAttrTeDefaultMetric;
import org.onosproject.bgpio.types.attr.BgpLinkAttrUnRsrvdLinkBandwidth;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcep.api.TeLinkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static org.onosproject.bgp.controller.BgpDpid.uri;
import static org.onosproject.incubator.net.resource.label.LabelResourceId.labelResourceId;
import static org.onosproject.net.Device.Type.ROUTER;
import static org.onosproject.net.Device.Type.VIRTUAL;
import static org.onosproject.net.DeviceId.deviceId;

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
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceAdminService labelResourceAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    private DeviceProviderService deviceProviderService;
    private LinkProviderService linkProviderService;

    private DeviceListener deviceListener = new InternalDeviceListener();
    private InternalBgpProvider listener = new InternalBgpProvider();
    private static final String UNKNOWN = "unknown";
    public static final long IDENTIFIER_SET = 0x100000000L;
    public static final String AS_NUMBER = "asNumber";
    public static final String DOMAIN_IDENTIFIER = "domainIdentifier";
    public static final String ROUTING_UNIVERSE = "routingUniverse";

    public static final String EXTERNAL_BIT = "externalBit";
    public static final String ABR_BIT = "abrBit";
    public static final String INTERNAL_BIT = "internalBit";
    public static final String PSEUDO = "psuedo";
    public static final String AREAID = "areaId";
    public static final String LSRID = "lsrId";
    public static final String COST = "cost";
    public static final String TE_COST = "teCost";

    public static final long PSEUDO_PORT = 0xffffffff;
    public static final int DELAY = 2;
    private LabelResourceId beginLabel = labelResourceId(5122);
    private LabelResourceId endLabel = labelResourceId(9217);
    private HashMap<DeviceId, List<PortDescription>> portMap = new HashMap<>();

    @Activate
    public void activate() {
        log.debug("BgpTopologyProvider activate");
        deviceProviderService = deviceProviderRegistry.register(this);
        linkProviderService = linkProviderRegistry.register(this);
        controller.addListener(listener);
        deviceService.addListener(deviceListener);
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
        deviceService.removeListener(deviceListener);
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();

            switch (event.type()) {
                case DEVICE_ADDED:
                    if (!mastershipService.isLocalMaster(device.id())) {
                        break;
                    }

                    // Reserve device label pool for L3 devices
                    if (device.annotations().value(LSRID) != null) {
                        createDevicePool(device.id());
                    }
                    break;

                default:
                    break;
            }
        }
    }

    /*
     * Implements device and link update.
     */
    private class InternalBgpProvider implements BgpNodeListener, BgpLinkListener {

        @Override
        public void addNode(BgpNodeLSNlriVer4 nodeNlri, PathAttrNlriDetails details) {
            log.debug("Add node {}", nodeNlri.toString());

            if (deviceProviderService == null || deviceService == null) {
                return;
            }
            Device.Type deviceType = ROUTER;
            BgpDpid nodeUri = new BgpDpid(nodeNlri);
            DeviceId deviceId = deviceId(uri(nodeUri.toString()));
            ChassisId cId = new ChassisId();

            /*
             * Check if device is already there (available) , if yes not updating to core.
             */
            if (deviceService.isAvailable(deviceId)) {
                return;
            }

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
                        newBuilder.set(AnnotationKeys.ROUTER_ID, nodeUri.isoNodeIdString(((IsIsPseudonode) tlv)
                                .getIsoNodeId()));
                    } else if (tlv instanceof OspfPseudonode) {
                        deviceType = VIRTUAL;
                        newBuilder
                                .set(AnnotationKeys.ROUTER_ID, Integer.toString(((OspfPseudonode) tlv).getrouterID()));
                    } else if (tlv instanceof IsIsNonPseudonode) {
                        newBuilder.set(AnnotationKeys.ROUTER_ID, nodeUri.isoNodeIdString(((IsIsNonPseudonode) tlv)
                                .getIsoNodeId()));
                    } else if (tlv instanceof OspfNonPseudonode) {
                        newBuilder.set(AnnotationKeys.ROUTER_ID,
                                Integer.toString(((OspfNonPseudonode) tlv).getrouterID()));
                    }
                }
            }
            DefaultAnnotations.Builder anntotations = DefaultAnnotations.builder();
            anntotations = getAnnotations(newBuilder, true, details);

            DeviceDescription description = new DefaultDeviceDescription(uri(nodeUri.toString()), deviceType, UNKNOWN,
                    UNKNOWN, UNKNOWN, UNKNOWN, cId, anntotations.build());

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

            if (labelResourceAdminService != null) {
                //Destroy local device label pool reserved for that device
                labelResourceAdminService.destroyDevicePool(deviceId);
            }

            deviceProviderService.deviceDisconnected(deviceId);
        }

        private List<PortDescription> buildPortDescriptions(DeviceId deviceId,
                                                            PortNumber portNumber) {

            List<PortDescription> portList;

            if (portMap.containsKey(deviceId)) {
                portList = portMap.get(deviceId);
            } else {
                portList = new ArrayList<>();
            }
            if (portNumber != null) {
                PortDescription portDescriptions = new DefaultPortDescription(portNumber, true);
                portList.add(portDescriptions);
            }

            portMap.put(deviceId, portList);
            return portList;
        }

        @Override
        public void addLink(BgpLinkLsNlriVer4 linkNlri, PathAttrNlriDetails details) throws BgpParseException {
            log.debug("Addlink {}", linkNlri.toString());

            LinkDescription linkDes = buildLinkDes(linkNlri, details, true);



            /*
             * Update link ports and configure bandwidth on source and destination port using networkConfig service
             * Only master of source link registers for bandwidth
             */
            if (mastershipService.isLocalMaster(linkDes.src().deviceId())) {
                registerBandwidthAndTeMetric(linkDes, details);
            }

            //Updating ports of the link
            deviceProviderService.updatePorts(linkDes.src().deviceId(), buildPortDescriptions(linkDes.src().deviceId(),
                    linkDes.src().port()));

            deviceProviderService.updatePorts(linkDes.dst().deviceId(), buildPortDescriptions(linkDes.dst().deviceId(),
                    linkDes.dst().port()));

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
            DefaultAnnotations.Builder annotationBuilder = DefaultAnnotations.builder();
            if (details != null) {
                annotationBuilder = getAnnotations(annotationBuilder, false, details);
            }

            return new DefaultLinkDescription(src, dst, Link.Type.DIRECT, false, annotationBuilder.build());
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

            /*
             * Only master for the link src will release the bandwidth resource.
             */
            if (networkConfigService != null && mastershipService.isLocalMaster(linkDes.src().deviceId())) {
                // Releases registered resource for this link
                networkConfigService.removeConfig(LinkKey.linkKey(linkDes.src(), linkDes.dst()), TeLinkConfig.class);
            }

            linkProviderService.linkVanished(linkDes);

            linkDes = new DefaultLinkDescription(linkDes.dst(), linkDes.src(), Link.Type.DIRECT,
                    false, linkDes.annotations());
            linkProviderService.linkVanished(linkDes);
            if (networkConfigService != null && mastershipService.isLocalMaster(linkDes.src().deviceId())) {
                // Releases registered resource for this link
                networkConfigService.removeConfig(LinkKey.linkKey(linkDes.src(), linkDes.dst()), TeLinkConfig.class);
            }

        }
    }

    // Creates label resource pool for the specific device. For all devices label range is 5122-9217
    private void createDevicePool(DeviceId deviceId) {
        if (labelResourceAdminService == null) {
            return;
        }

        labelResourceAdminService.createDevicePool(deviceId, beginLabel, endLabel);
    }

    private void registerBandwidthAndTeMetric(LinkDescription linkDes, PathAttrNlriDetails details) {
        if (details ==  null) {
            log.error("Couldnot able to register bandwidth ");
            return;
        }

        List<BgpValueType> attribute = details.pathAttributes().stream()
                .filter(attr -> attr instanceof LinkStateAttributes).collect(toList());
        if (attribute.isEmpty()) {
            return;
        }

        List<BgpValueType> tlvs = ((LinkStateAttributes) attribute.iterator().next()).linkStateAttributes();
        double maxReservableBw = 0;
        List<Float>  unreservedBw = new ArrayList<>();
        int teMetric = 0;
        int igpMetric = 0;

        for (BgpValueType tlv : tlvs) {
            switch (tlv.getType()) {
            case LinkStateAttributes.ATTR_LINK_MAX_RES_BANDWIDTH:
                maxReservableBw = ((BgpLinkAttrMaxLinkBandwidth) tlv).linkAttrMaxLinkBandwidth();
                //will get in bits/second , convert to MBPS to store in network config service
                maxReservableBw = maxReservableBw / 1000000;
                break;
            case LinkStateAttributes.ATTR_LINK_UNRES_BANDWIDTH:
                unreservedBw = ((BgpLinkAttrUnRsrvdLinkBandwidth) tlv).getLinkAttrUnRsrvdLinkBandwidth();
                break;
            case LinkStateAttributes.ATTR_LINK_TE_DEFAULT_METRIC:
                teMetric = ((BgpLinkAttrTeDefaultMetric) tlv).attrLinkDefTeMetric();
                break;
            case LinkStateAttributes.ATTR_LINK_IGP_METRIC:
                igpMetric = ((BgpLinkAttrIgpMetric) tlv).attrLinkIgpMetric();
                break;
            default: // do nothing
            }
        }

        //Configure bandwidth for src and dst port
        TeLinkConfig config = networkConfigService.addConfig(LinkKey.linkKey(linkDes.src(), linkDes.dst()),
                                                             TeLinkConfig.class);
        Double bw = 0.0;
        if (unreservedBw.size() > 0) {
            bw = unreservedBw.get(0).doubleValue(); //Low priority
        }

        config.maxResvBandwidth(maxReservableBw)
                .unResvBandwidth(bw).teCost(teMetric).igpCost(igpMetric);
                //.apply();

        networkConfigService.applyConfig(LinkKey.linkKey(linkDes.src(),
                linkDes.dst()), TeLinkConfig.class, config.node());
    }

    private DefaultAnnotations.Builder getAnnotations(DefaultAnnotations.Builder annotationBuilder, boolean isNode,
            PathAttrNlriDetails details) {

        List<BgpValueType> attribute = details.pathAttributes().stream()
                .filter(attr -> attr instanceof LinkStateAttributes).collect(toList());
        if (attribute.isEmpty()) {
            return annotationBuilder;
        }
        List<BgpValueType> tlvs = ((LinkStateAttributes) attribute.iterator().next()).linkStateAttributes();
        boolean abrBit = false;
        boolean externalBit = false;
        boolean pseudo = false;
        byte[] areaId = null;
        Ip4Address routerId = null;
        for (BgpValueType tlv : tlvs) {
            switch (tlv.getType()) {
            case LinkStateAttributes.ATTR_NODE_FLAG_BITS:
                abrBit = ((BgpAttrNodeFlagBitTlv) tlv).abrBit();
                externalBit = ((BgpAttrNodeFlagBitTlv) tlv).externalBit();
                break;
            case NodeDescriptors.IGP_ROUTERID_TYPE:
                if (tlv instanceof IsIsPseudonode || tlv instanceof OspfPseudonode) {
                    pseudo = true;
                }
                break;
            case LinkStateAttributes.ATTR_NODE_ISIS_AREA_ID:
                areaId = ((BgpAttrNodeIsIsAreaId) tlv).attrNodeIsIsAreaId();
                break;
            case LinkStateAttributes.ATTR_NODE_IPV4_LOCAL_ROUTER_ID:
                routerId = ((BgpAttrRouterIdV4) tlv).attrRouterId();
                break;
            default: // do nothing
            }
        }

        // Annotations for device
        if (isNode) {
            boolean internalBit = false;
            if (!abrBit && !externalBit) {
                internalBit = true;
            }

            annotationBuilder.set(EXTERNAL_BIT, String.valueOf(externalBit));
            annotationBuilder.set(ABR_BIT, String.valueOf(abrBit));
            annotationBuilder.set(INTERNAL_BIT, String.valueOf(internalBit));
            annotationBuilder.set(PSEUDO, String.valueOf(pseudo));

            if (areaId != null) {
                annotationBuilder.set(AREAID, new String(areaId));
            }
            if (routerId != null) {
                // LsrID
                annotationBuilder.set(LSRID, String.valueOf(routerId));
            }
        }
        return annotationBuilder;
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
