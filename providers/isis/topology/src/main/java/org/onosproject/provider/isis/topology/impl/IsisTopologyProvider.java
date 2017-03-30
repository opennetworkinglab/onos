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

package org.onosproject.provider.isis.topology.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onlab.util.Bandwidth;
import org.onosproject.isis.controller.IsisController;
import org.onosproject.isis.controller.topology.IsisLink;
import org.onosproject.isis.controller.topology.IsisLinkListener;
import org.onosproject.isis.controller.topology.IsisLinkTed;
import org.onosproject.isis.controller.topology.IsisRouter;
import org.onosproject.isis.controller.topology.IsisRouterId;
import org.onosproject.isis.controller.topology.IsisRouterListener;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BandwidthCapacity;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which advertises device descriptions to the core.
 */
@Component(immediate = true)
public class IsisTopologyProvider extends AbstractProvider implements DeviceProvider, LinkProvider {

    public static final long PSEUDO_PORT = 0xffffffff;
    public static final String ADMINISTRATIVEGROUP = "administrativeGroup";
    public static final String TE_METRIC = "teMetric";
    public static final String MAXRESERVABLEBANDWIDTH = "maxReservableBandwidth";
    public static final String ROUTERID = "routerId";
    public static final String NEIGHBORID = "neighborId";
    private static final Logger log = getLogger(IsisTopologyProvider.class);
    // Default values for tunable parameters
    private static final String UNKNOWN = "unknown";
    final InternalTopologyProvider listener = new InternalTopologyProvider();
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IsisController controller;
    //This Interface that defines how this provider can interact with the core.
    private LinkProviderService linkProviderService;
    // The interface that defines how this Provider can interact with the core
    private DeviceProviderService deviceProviderService;
    private HashMap<DeviceId, List<PortDescription>> portMap = new HashMap<>();

    /**
     * Creates an ISIS device provider.
     */
    public IsisTopologyProvider() {
        super(new ProviderId("l3", "org.onosproject.provider.isis"));
    }

    @Activate
    public void activate() {
        deviceProviderService = deviceProviderRegistry.register(this);
        linkProviderService = linkProviderRegistry.register(this);
        controller.addRouterListener(listener);
        controller.addLinkListener(listener);
        log.debug("IsisDeviceProvider::activate...!!!!");
    }

    @Deactivate
    public void deactivate() {
        log.debug("IsisDeviceProvider::deactivate...!!!!");
        deviceProviderRegistry.unregister(this);
        deviceProviderService = null;
        linkProviderRegistry.unregister(this);
        linkProviderService = null;
        controller.removeRouterListener(listener);
        controller.removeLinkListener(listener);
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        log.debug("IsisDeviceProvider::triggerProbe...!!!!");
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        log.debug("IsisDeviceProvider::roleChanged...!!!!");
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        log.debug("IsisDeviceProvider::isReachable...!!!!");
        return true;
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber, boolean enable) {
        log.debug("IsisDeviceProvider::changePortState...!!!!");
    }

    /**
     * Builds link description.
     *
     * @param isisLink ISIS link instance
     * @return link description
     */
    private LinkDescription buildLinkDes(IsisLink isisLink) {
        checkNotNull(isisLink);
        long srcAddress = 0;
        long dstAddress = 0;
        boolean localPseduo = false;
        boolean remotePseduo = false;
        String localSystemId = isisLink.localSystemId();
        String remoteSystemId = isisLink.remoteSystemId();
        //Changing of port numbers
        if (isisLink.interfaceIp() != null) {
            //srcAddress = isisLink.interfaceIp().toInt();
            srcAddress = (long) Long.parseUnsignedLong(Integer.toBinaryString(isisLink.interfaceIp().toInt()), 2);
        }
        if (isisLink.neighborIp() != null) {
            //dstAddress = isisLink.neighborIp().toInt();
            dstAddress = (long) Long.parseUnsignedLong(Integer.toBinaryString(isisLink.neighborIp().toInt()), 2);
        }
        DeviceId srcId = DeviceId.deviceId(IsisRouterId.uri(localSystemId));
        DeviceId dstId = DeviceId.deviceId(IsisRouterId.uri(remoteSystemId));
        if (checkIsDis(isisLink.localSystemId())) {
            localPseduo = true;
        } else if (checkIsDis(isisLink.remoteSystemId())) {
            remotePseduo = true;
        } else {
            log.debug("IsisDeviceProvider::buildLinkDes : unknown type.!");
        }

        if (localPseduo && srcAddress == 0) {
            srcAddress = PSEUDO_PORT;
        } else if (remotePseduo && dstAddress == 0) {
            dstAddress = PSEUDO_PORT;
        }

        ConnectPoint src = new ConnectPoint(srcId, PortNumber.portNumber(srcAddress));
        ConnectPoint dst = new ConnectPoint(dstId, PortNumber.portNumber(dstAddress));
        DefaultAnnotations.Builder annotationBuilder = DefaultAnnotations.builder();

        annotationBuilder = buildAnnotations(annotationBuilder, isisLink);

        return new DefaultLinkDescription(src, dst, Link.Type.DIRECT, false, annotationBuilder.build());
    }

    /**
     * Return the DIS value from the systemId.
     *
     * @param systemId system Id.
     * @return return true if DIS else false
     */
    public static boolean checkIsDis(String systemId) {
        StringTokenizer stringTokenizer = new StringTokenizer(systemId, "." + "-");
        int count = 0;
        while (stringTokenizer.hasMoreTokens()) {
            String str = stringTokenizer.nextToken();
            if (count == 3) {
                int x = Integer.parseInt(str);
                if (x > 0) {
                    return true;
                }
            }
            count++;
        }
        return false;
    }

    /**
     * Builds port description.
     *
     * @param deviceId   device ID for the port
     * @param portNumber port number of the link
     * @return list of port description
     */
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

    /**
     * Builds the annotation details.
     *
     * @param annotationBuilder default annotation builder instance
     * @param isisLink          ISIS link instance
     * @return annotation builder instance
     */
    private DefaultAnnotations.Builder buildAnnotations(DefaultAnnotations.Builder annotationBuilder,
                                                        IsisLink isisLink) {
        int administrativeGroup = 0;
        long teMetric = 0;
        Bandwidth maxReservableBandwidth = Bandwidth.bps(0);
        String routerId = null;
        String neighborId = null;

        //TE Info
        IsisLinkTed isisLinkTed = isisLink.linkTed();
        log.info("Ted Information:  {}", isisLinkTed.toString());
        administrativeGroup = isisLinkTed.administrativeGroup();
        teMetric = isisLinkTed.teDefaultMetric();
        maxReservableBandwidth = isisLinkTed.maximumReservableLinkBandwidth();
        routerId = isisLink.localSystemId();
        neighborId = isisLink.remoteSystemId();
        annotationBuilder.set(ADMINISTRATIVEGROUP, String.valueOf(administrativeGroup));
        annotationBuilder.set(TE_METRIC, String.valueOf(teMetric));
        annotationBuilder.set(MAXRESERVABLEBANDWIDTH, String.valueOf(maxReservableBandwidth));
        annotationBuilder.set(ROUTERID, String.valueOf(routerId));
        annotationBuilder.set(NEIGHBORID, String.valueOf(neighborId));
        return annotationBuilder;
    }

    /**
     * Internal device provider implementation.
     */
    private class InternalTopologyProvider implements IsisRouterListener, IsisLinkListener {

        @Override
        public void routerAdded(IsisRouter isisRouter) {
            String systemId = isisRouter.systemId();
            log.info("Added device {}", systemId);
            DeviceId deviceId = DeviceId.deviceId(IsisRouterId.uri(systemId));
            Device.Type deviceType = Device.Type.ROUTER;
            //If our routerType is Dr or Bdr type is PSEUDO
            if (isisRouter.isDis()) {
                deviceType = Device.Type.ROUTER;
            } else {
                deviceType = Device.Type.VIRTUAL;
            }
            ChassisId cId = new ChassisId();
            DefaultAnnotations.Builder newBuilder = DefaultAnnotations.builder();
            newBuilder.set(AnnotationKeys.TYPE, "L3");
            newBuilder.set("RouterId", systemId);
            DeviceDescription description =
                    new DefaultDeviceDescription(IsisRouterId.uri(systemId), deviceType, UNKNOWN, UNKNOWN, UNKNOWN,
                                                 UNKNOWN, cId, newBuilder.build());
            deviceProviderService.deviceConnected(deviceId, description);
            System.out.println("Device added: " + systemId);
        }

        @Override
        public void routerRemoved(IsisRouter isisRouter) {
            String systemId = isisRouter.systemId();
            log.info("Delete device {}", systemId);
            DeviceId deviceId = DeviceId.deviceId(IsisRouterId.uri(systemId));
            if (deviceProviderService == null) {
                return;
            }
            deviceProviderService.deviceDisconnected(deviceId);
            log.info("delete device {}", systemId);
        }

        @Override
        public void addLink(IsisLink isisLink) {
            log.debug("Addlink {}", isisLink.localSystemId());

            LinkDescription linkDes = buildLinkDes(isisLink);
            //Updating ports of the link
            //If already link exists, return
            if (linkService.getLink(linkDes.src(), linkDes.dst()) != null || linkProviderService == null) {
                return;
            }
            ConnectPoint destconnectPoint = linkDes.dst();
            PortNumber destport = destconnectPoint.port();
            if (destport.toLong() != 0) {
                deviceProviderService.updatePorts(linkDes.src().deviceId(),
                                                  buildPortDescriptions(linkDes.src().deviceId(),
                                                  linkDes.src().port()));
                deviceProviderService.updatePorts(linkDes.dst().deviceId(),
                                                  buildPortDescriptions(linkDes.dst().deviceId(),
                                                  linkDes.dst().port()));
                registerBandwidth(linkDes, isisLink);
                linkProviderService.linkDetected(linkDes);
                System.out.println("link desc " + linkDes.toString());
            }
        }

        @Override
        public void deleteLink(IsisLink isisLink) {
            log.debug("Delete link {}", isisLink.localSystemId());
            if (linkProviderService == null) {
                return;
            }
            LinkDescription linkDes = buildLinkDes(isisLink);
            linkProviderService.linkVanished(linkDes);
        }

        /**
         * Registers the bandwidth for source and destination points.
         *
         * @param linkDes  link description instance
         * @param isisLink ISIS link instance
         */
        private void registerBandwidth(LinkDescription linkDes, IsisLink isisLink) {
            if (isisLink == null) {
                log.error("Could not able to register bandwidth ");
                return;
            }
            IsisLinkTed isisLinkTed = isisLink.linkTed();
            Bandwidth maxReservableBw = isisLinkTed.maximumReservableLinkBandwidth();
            if (maxReservableBw != null) {
                if (maxReservableBw.compareTo(Bandwidth.bps(0)) == 0) {
                    return;
                }
                //Configure bandwidth for src and dst port
                BandwidthCapacity config = networkConfigService.addConfig(linkDes.src(), BandwidthCapacity.class);
                config.capacity(maxReservableBw).apply();

                config = networkConfigService.addConfig(linkDes.dst(), BandwidthCapacity.class);
                config.capacity(maxReservableBw).apply();
            }
        }
    }
}