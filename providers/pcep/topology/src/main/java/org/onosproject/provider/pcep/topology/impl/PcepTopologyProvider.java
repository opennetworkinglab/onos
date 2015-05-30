/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.pcep.topology.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.pcep.api.PcepDpid.uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.cluster.ClusterService;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Link.Type;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
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
import org.onosproject.pcep.api.PcepController;
import org.onosproject.pcep.api.PcepDpid;
import org.onosproject.pcep.api.PcepLink;
import org.onosproject.pcep.api.PcepLink.PortType;
import org.onosproject.pcep.api.PcepLinkListener;
import org.onosproject.pcep.api.PcepOperator.OperationType;
import org.onosproject.pcep.api.PcepSwitch;
import org.onosproject.pcep.api.PcepSwitchListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider which uses an PCEP controller to detect network infrastructure
 * topology.
 */
@Component(immediate = true)
public class PcepTopologyProvider extends AbstractProvider
        implements LinkProvider, DeviceProvider {

    public PcepTopologyProvider() {
        super(new ProviderId("pcep", "org.onosproject.provider.pcep"));
    }

    private static final Logger log = LoggerFactory
            .getLogger(PcepTopologyProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry linkProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PcepController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipAdminService mastershipAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private DeviceProviderService deviceProviderService;
    private LinkProviderService linkProviderService;
    // List<Long> srcportList = new ArrayList<Long>();
    HashSet<Long> portSet = new HashSet<>();
    private InternalLinkProvider listener = new InternalLinkProvider();

    @Activate
    public void activate() {
        linkProviderService = linkProviderRegistry.register(this);
        deviceProviderService = deviceProviderRegistry.register(this);
        controller.addListener(listener);
        controller.addLinkListener(listener);
    }

    @Deactivate
    public void deactivate() {
        linkProviderRegistry.unregister(this);
        linkProviderService = null;
        controller.removeListener(listener);
        controller.removeLinkListener(listener);
    }

    private List<PortDescription> buildPortDescriptions(List<Long> ports,
                                                        PortType portType) {
        final List<PortDescription> portDescs = new ArrayList<>();
        for (long port : ports) {
            portDescs.add(buildPortDescription(port, portType));
        }
        return portDescs;
    }

    private PortDescription buildPortDescription(long port, PortType portType) {
        final PortNumber portNo = PortNumber.portNumber(port);
        final boolean enabled = true;
        DefaultAnnotations extendedAttributes = DefaultAnnotations.builder()
                .set("portType", String.valueOf(portType)).build();
        return new DefaultPortDescription(portNo, enabled, extendedAttributes);
    }

    /**
     * Build link annotations from pcep link description.the annotations consist
     * of lots of property of Huawei device.
     *
     * @param linkDesc
     * @return
     */
    private DefaultAnnotations buildLinkAnnotations(PcepLink linkDesc) {
        DefaultAnnotations extendedAttributes = DefaultAnnotations
                .builder()
                .set("subType", String.valueOf(linkDesc.linkSubType()))
                .set("workState", linkDesc.linkState())
                .set("distance", String.valueOf(linkDesc.linkDistance()))
                .set("capType", linkDesc.linkCapacityType().toLowerCase())
                .set("avail_" + linkDesc.linkCapacityType().toLowerCase(),
                     String.valueOf(linkDesc.linkAvailValue()))
                .set("max_" + linkDesc.linkCapacityType().toLowerCase(),
                     String.valueOf(linkDesc.linkMaxValue())).build();

        return extendedAttributes;
    }

    /**
     * Build a LinkDescription from a PCEPLink.
     *
     * @param pceLink
     * @return LinkDescription
     */
    private LinkDescription buildLinkDescription(PcepLink pceLink) {
        LinkDescription ld;

        DeviceId srcDeviceID = deviceId(uri(pceLink.linkSrcDeviceID()));
        DeviceId dstDeviceID = deviceId(uri(pceLink.linkDstDeviceId()));

        if (deviceService.getDevice(srcDeviceID) == null
                || deviceService.getDevice(dstDeviceID) == null) {
            log.info("the device of the link is not exited" + srcDeviceID
                    + dstDeviceID);
            return null;
        }
        // update port info
        long srcPort = pceLink.linkSrcPort();
        portSet.add(srcPort);
        List<Long> srcportList = new ArrayList<Long>();
        srcportList.addAll(portSet);
        deviceProviderService
                .updatePorts(srcDeviceID,
                             buildPortDescriptions(srcportList,
                                                   pceLink.portType()));

        ConnectPoint src = new ConnectPoint(srcDeviceID,
                                            PortNumber.portNumber(pceLink
                                                    .linkSrcPort()));

        ConnectPoint dst = new ConnectPoint(dstDeviceID,
                                            PortNumber.portNumber(pceLink
                                                    .linkDstPort()));
        DefaultAnnotations extendedAttributes = buildLinkAnnotations(pceLink);

        // construct the link
        ld = new DefaultLinkDescription(src, dst, Type.OPTICAL,
                                        extendedAttributes);
        return ld;
    }

    private void processLinkUpdate(LinkDescription linkDescription) {

        // dst changed, delete the original link,if the dst device is not in
        // other links ,delete it.
        if (linkService.getLink(linkDescription.src(), linkDescription.dst()) == null) {
            // in face,one src one link
            Set<Link> links = linkService
                    .getIngressLinks(linkDescription.src());
            for (Link link : links) {
                linkProviderService.linkVanished((LinkDescription) link);
                if (linkService.getDeviceLinks(link.dst().deviceId()).size() == 0) {
                    deviceProviderService.deviceDisconnected(link.dst()
                            .deviceId());
                }
            }

        }
        linkProviderService.linkDetected(linkDescription);

    }

    private class InternalLinkProvider
            implements PcepSwitchListener, PcepLinkListener {

        @Override
        public void switchAdded(PcepDpid dpid) {
            // TODO Auto-generated method stub

            if (deviceProviderService == null) {
                return;
            }
            DeviceId devicdId = deviceId(uri(dpid));
            PcepSwitch sw = controller.getSwitch(dpid);
            checkNotNull(sw, "device should not null.");
            // The default device type is switch.
            ChassisId cId = new ChassisId(dpid.value());
            Device.Type deviceType = null;

            switch (sw.getDeviceType()) {
            case ROADM:
                deviceType = Device.Type.ROADM;
                break;
            case OTN:
                deviceType = Device.Type.SWITCH;
                break;
            case ROUTER:
                deviceType = Device.Type.ROUTER;
                break;
            default:
                deviceType = Device.Type.OTHER;
            }

            DeviceDescription description = new DefaultDeviceDescription(
                                                                         devicdId.uri(),
                                                                         deviceType,
                                                                         sw.manufacturerDescription(),
                                                                         sw.hardwareDescription(),
                                                                         sw.softwareDescription(),
                                                                         sw.serialNumber(),
                                                                         cId);
            deviceProviderService.deviceConnected(devicdId, description);

        }

        @Override
        public void switchRemoved(PcepDpid dpid) {
            // TODO Auto-generated method stub

            if (deviceProviderService == null || linkProviderService == null) {
                return;
            }
            deviceProviderService.deviceDisconnected(deviceId(uri(dpid)));

            linkProviderService.linksVanished(DeviceId.deviceId(uri(dpid)));
        }

        @Override
        public void switchChanged(PcepDpid dpid) {
            // TODO Auto-generated method stub

        }

        @Override
        public void handlePCEPlink(PcepLink link) {

            OperationType operType = link.getOperationType();
            LinkDescription ld = buildLinkDescription(link);
            if (ld == null) {
                log.error("Invalid link info.");
                return;
            }
            switch (operType) {
            case ADD:
                linkProviderService.linkDetected(ld);
                break;
            case UPDATE:
                processLinkUpdate(ld);
                break;

            case DELETE:
                linkProviderService.linkVanished(ld);
                break;

            default:
                break;

            }
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
}
