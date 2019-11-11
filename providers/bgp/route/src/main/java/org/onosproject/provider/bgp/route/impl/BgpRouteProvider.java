/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.provider.bgp.route.impl;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpId;
import org.onosproject.bgp.controller.BgpPeer.FlowSpecOperation;
import org.onosproject.bgp.controller.BgpRouteListener;
import org.onosproject.bgpio.protocol.BgpEvpnNlri;
import org.onosproject.bgpio.protocol.BgpUpdateMsg;
import org.onosproject.bgpio.protocol.evpn.BgpEvpnNlriImpl;
import org.onosproject.bgpio.protocol.evpn.BgpEvpnRouteType;
import org.onosproject.bgpio.protocol.evpn.BgpEvpnRouteType2Nlri;
import org.onosproject.bgpio.types.BgpEvpnEsi;
import org.onosproject.bgpio.types.BgpEvpnLabel;
import org.onosproject.bgpio.types.BgpExtendedCommunity;
import org.onosproject.bgpio.types.BgpNlriType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.MpUnReachNlri;
import org.onosproject.bgpio.types.RouteDistinguisher;
import org.onosproject.bgpio.types.RouteTarget;
import org.onosproject.evpnrouteservice.EvpnRoute;
import org.onosproject.evpnrouteservice.EvpnRoute.Source;
import org.onosproject.evpnrouteservice.EvpnRouteAdminService;
import org.onosproject.evpnrouteservice.EvpnRouteEvent;
import org.onosproject.evpnrouteservice.EvpnRouteListener;
import org.onosproject.evpnrouteservice.VpnRouteTarget;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Provider which uses an BGP controller to update/delete route.
 */
@Component(immediate = true)
public class BgpRouteProvider extends AbstractProvider {

    /**
     * Creates an instance of BGP route provider.
     */
    public BgpRouteProvider() {
        super(new ProviderId("route",
                             "org.onosproject.provider.bgp.route.impl"));
    }

    private static final Logger log = LoggerFactory
            .getLogger(BgpRouteProvider.class);


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected BgpController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EvpnRouteAdminService evpnRouteAdminService;

    private final InternalEvpnRouteListener routeListener = new
            InternalEvpnRouteListener();
    private final InternalBgpRouteListener bgpRouteListener = new
            InternalBgpRouteListener();


    @Activate
    public void activate() {
        controller.addRouteListener(bgpRouteListener);
        evpnRouteAdminService.addListener(routeListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        controller.removeRouteListener(bgpRouteListener);
        evpnRouteAdminService.removeListener(routeListener);
        log.info("Stopped");
    }

    /**
     * Handles the bgp route update message.
     *
     * @param operationType operationType
     * @param rdString      rd
     * @param exportRtList  rt export
     * @param nextHop       next hop
     * @param macAddress    mac address
     * @param ipAddress     ip address
     * @param labelInt      label
     */
    private void sendUpdateMessage(FlowSpecOperation operationType,
                                   String rdString,
                                   List<VpnRouteTarget> exportRtList,
                                   IpAddress nextHop,
                                   MacAddress macAddress,
                                   InetAddress ipAddress,
                                   int labelInt) {
        log.info("sendUpdateMessage 1");

        List<BgpEvpnNlri> eVpnNlri = new ArrayList<BgpEvpnNlri>();
        RouteDistinguisher rd = stringToRD(rdString);
        BgpEvpnEsi esi = new BgpEvpnEsi(new byte[10]);
        int ethernetTagID = 0;
        BgpEvpnLabel mplsLabel1 = intToLabel(labelInt);
        BgpEvpnLabel mplsLabel2 = null;

        List<BgpValueType> extCom = new ArrayList<BgpValueType>();
        if ((operationType == FlowSpecOperation.UPDATE)
                && (!exportRtList.isEmpty())) {
            for (VpnRouteTarget rt : exportRtList) {
                RouteTarget rTarget = stringToRT(rt.getRouteTarget());
                extCom.add(rTarget);
            }
        }
        BgpEvpnRouteType2Nlri routeTypeSpec =
                new BgpEvpnRouteType2Nlri(rd,
                                          esi,
                                          ethernetTagID,
                                          macAddress,
                                          ipAddress,
                                          mplsLabel1,
                                          mplsLabel2);
        BgpEvpnNlri nlri = new BgpEvpnNlriImpl(BgpEvpnRouteType
                                                       .MAC_IP_ADVERTISEMENT
                                                       .getType(),
                                               routeTypeSpec);
        eVpnNlri.add(nlri);
        log.info("sendUpdateMessage 2");
        controller.getPeers().forEach(peer -> {
            log.info("Send route update eVpnComponents {} to peer {}",
                     eVpnNlri, peer);
            peer.updateEvpnNlri(operationType, nextHop, extCom, eVpnNlri);
        });

    }

    private static RouteDistinguisher stringToRD(String rdString) {
        if (rdString.contains(":")) {
            if ((rdString.indexOf(":") != 0)
                    && (rdString.indexOf(":") != rdString.length() - 1)) {
                String[] tem = rdString.split(":");
                short as = (short) Integer.parseInt(tem[0]);
                int assignednum = Integer.parseInt(tem[1]);
                long rd = ((long) assignednum & 0xFFFFFFFFL)
                        | (((long) as << 32) & 0xFFFFFFFF00000000L);
                return new RouteDistinguisher(rd);
            }
        }
        return null;

    }

    private static String rdToString(RouteDistinguisher rd) {
        long rdLong = rd.getRouteDistinguisher();
        int as = (int) ((rdLong & 0xFFFFFFFF00000000L) >> 32);
        int assignednum = (int) (rdLong & 0xFFFFFFFFL);
        String result = as + ":" + assignednum;
        return result;
    }

    private static RouteTarget stringToRT(String rdString) {
        if (rdString.contains(":")) {
            if ((rdString.indexOf(":") != 0)
                    && (rdString.indexOf(":") != rdString.length() - 1)) {
                String[] tem = rdString.split(":");
                short as = Short.parseShort(tem[0]);
                int assignednum = Integer.parseInt(tem[1]);

                byte[] rt = new byte[]{(byte) ((as >> 8) & 0xFF),
                        (byte) (as & 0xFF),
                        (byte) ((assignednum >> 24) & 0xFF),
                        (byte) ((assignednum >> 16) & 0xFF),
                        (byte) ((assignednum >> 8) & 0xFF),
                        (byte) (assignednum & 0xFF)};
                short type = 0x02;
                return new RouteTarget(type, rt);
            }
        }
        return null;

    }

    private static String rtToString(RouteTarget rt) {
        byte[] b = rt.getRouteTarget();

        int assignednum = b[5] & 0xFF | (b[4] & 0xFF) << 8 | (b[3] & 0xFF) << 16
                | (b[2] & 0xFF) << 24;
        short as = (short) (b[1] & 0xFF | (b[0] & 0xFF) << 8);
        String result = as + ":" + assignednum;
        return result;
    }

    private static BgpEvpnLabel intToLabel(int labelInt) {
        byte[] label = new byte[]{(byte) ((labelInt >> 16) & 0xFF),
                (byte) ((labelInt >> 8) & 0xFF),
                (byte) (labelInt & 0xFF)};

        return new BgpEvpnLabel(label);
    }

    private static int labelToInt(BgpEvpnLabel label) {
        byte[] b = label.getMplsLabel();
        return b[2] & 0xFF | (b[1] & 0xFF) << 8 | (b[0] & 0xFF) << 16;

    }

    private class InternalBgpRouteListener implements BgpRouteListener {

        @Override
        public void processRoute(BgpId bgpId, BgpUpdateMsg updateMsg) {
            log.info("Evpn route event received from BGP protocol");
            List<BgpValueType> pathAttr = updateMsg.bgpPathAttributes()
                    .pathAttributes();
            Iterator<BgpValueType> iterator = pathAttr.iterator();
            RouteTarget rt = null;
            List<VpnRouteTarget> exportRt = new LinkedList<>();
            List<BgpEvpnNlri> evpnReachNlri = new LinkedList<>();
            List<BgpEvpnNlri> evpnUnreachNlri = new LinkedList<>();

            IpAddress ipNextHop = null;
            while (iterator.hasNext()) {
                BgpValueType attr = iterator.next();
                if (attr instanceof MpReachNlri) {
                    MpReachNlri mpReachNlri = (MpReachNlri) attr;
                    ipNextHop = mpReachNlri.nexthop();
                    if (mpReachNlri
                            .getNlriDetailsType() == BgpNlriType.EVPN) {
                        evpnReachNlri.addAll(mpReachNlri.bgpEvpnNlri());
                    }

                }
                if (attr instanceof MpUnReachNlri) {
                    MpUnReachNlri mpUnReachNlri = (MpUnReachNlri) attr;
                    if (mpUnReachNlri
                            .getNlriDetailsType() == BgpNlriType.EVPN) {
                        evpnUnreachNlri.addAll(mpUnReachNlri.bgpEvpnNlri());
                    }
                }

                if (attr instanceof BgpExtendedCommunity) {
                    BgpExtendedCommunity extCom = (BgpExtendedCommunity) attr;
                    Iterator<BgpValueType> extIte = extCom.fsActionTlv()
                            .iterator();
                    while (extIte.hasNext()) {
                        BgpValueType extAttr = extIte.next();
                        if (extAttr instanceof RouteTarget) {
                            rt = (RouteTarget) extAttr;
                            exportRt.add(VpnRouteTarget
                                                 .routeTarget(rtToString(rt)));
                            break;
                        }
                    }
                }
            }

            if ((!exportRt.isEmpty()) && (!evpnReachNlri.isEmpty())) {
                for (BgpEvpnNlri nlri : evpnReachNlri) {
                    if (nlri.getRouteType() == BgpEvpnRouteType
                            .MAC_IP_ADVERTISEMENT) {
                        BgpEvpnRouteType2Nlri macIpAdvNlri
                                = (BgpEvpnRouteType2Nlri) nlri
                                .getNlri();
                        MacAddress macAddress = macIpAdvNlri.getMacAddress();
                        Ip4Address ipAddress = Ip4Address
                                .valueOf(macIpAdvNlri.getIpAddress());
                        RouteDistinguisher rd = macIpAdvNlri
                                .getRouteDistinguisher();
                        BgpEvpnLabel label = macIpAdvNlri.getMplsLable1();
                        log.info("Route Provider received bgp packet {} " +
                                         "to route system.",
                                 macIpAdvNlri.toString());
                        // Add route to route system
                        Source source = Source.REMOTE;
                        EvpnRoute evpnRoute = new EvpnRoute(source,
                                                            macAddress,
                                                            IpPrefix.valueOf(ipAddress, 32),
                                                            ipNextHop,
                                                            rdToString(rd),
                                                            null, //empty rt
                                                            exportRt,
                                                            labelToInt(label));

                        evpnRouteAdminService.update(Collections
                                                             .singleton(evpnRoute));
                    }
                }
            }

            if (!evpnUnreachNlri.isEmpty()) {
                for (BgpEvpnNlri nlri : evpnUnreachNlri) {
                    if (nlri.getRouteType() == BgpEvpnRouteType
                            .MAC_IP_ADVERTISEMENT) {
                        BgpEvpnRouteType2Nlri macIpAdvNlri
                                = (BgpEvpnRouteType2Nlri) nlri
                                .getNlri();
                        MacAddress macAddress = macIpAdvNlri.getMacAddress();
                        Ip4Address ipAddress = Ip4Address
                                .valueOf(macIpAdvNlri.getIpAddress());
                        RouteDistinguisher rd = macIpAdvNlri
                                .getRouteDistinguisher();
                        BgpEvpnLabel label = macIpAdvNlri.getMplsLable1();
                        log.info("Route Provider received bgp packet {} " +
                                         "and remove from route system.",
                                 macIpAdvNlri.toString());
                        // Delete route from route system
                        Source source = Source.REMOTE;
                        // For mpUnreachNlri, nexthop and rt is null
                        EvpnRoute evpnRoute = new EvpnRoute(source,
                                                            macAddress,
                                                            IpPrefix.valueOf(ipAddress, 32),
                                                            null,
                                                            rdToString(rd),
                                                            null,
                                                            null,
                                                            labelToInt(label));

                        evpnRouteAdminService.withdraw(Collections
                                                               .singleton(evpnRoute));
                    }
                }
            }
        }
    }

    private class InternalEvpnRouteListener implements EvpnRouteListener {

        @Override
        public void event(EvpnRouteEvent event) {
            log.info("evpnroute event is received from evpn route manager");
            FlowSpecOperation operationType = null;
            EvpnRoute route = event.subject();
            EvpnRoute evpnRoute = route;
            log.info("Event received for public route {}", evpnRoute);
            if (evpnRoute.source().equals(Source.REMOTE)) {
                return;
            }
            switch (event.type()) {
                case ROUTE_ADDED:
                case ROUTE_UPDATED:
                    log.info("route added");
                    operationType = FlowSpecOperation.UPDATE;
                    break;
                case ROUTE_REMOVED:
                    log.info("route deleted");
                    operationType = FlowSpecOperation.DELETE;
                    break;
                default:
                    break;
            }

            String rdString = evpnRoute.routeDistinguisher()
                    .getRouteDistinguisher();
            MacAddress macAddress = evpnRoute.prefixMac();
            InetAddress inetAddress = evpnRoute.prefixIp().address().toInetAddress();
            IpAddress nextHop = evpnRoute.ipNextHop();
            List<VpnRouteTarget> exportRtList = evpnRoute
                    .exportRouteTarget();
            int labelInt = evpnRoute.label().getLabel();

            sendUpdateMessage(operationType,
                              rdString,
                              exportRtList,
                              nextHop,
                              macAddress,
                              inetAddress,
                              labelInt);
        }
    }
}
