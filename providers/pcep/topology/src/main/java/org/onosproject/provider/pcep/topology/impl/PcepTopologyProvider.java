/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link.Type;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
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
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcep.api.DeviceCapability;
import org.onosproject.pcep.api.PcepController;
import org.onosproject.pcep.api.PcepDpid;
import org.onosproject.pcep.api.PcepLink;
import org.onosproject.pcep.api.PcepLinkListener;
import org.onosproject.pcep.api.PcepOperator.OperationType;
import org.onosproject.pcep.api.PcepSwitch;
import org.onosproject.pcep.api.PcepSwitchListener;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepNodeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.pcep.api.PcepDpid.uri;

/**
 * Provider which uses an PCEP controller to detect network infrastructure
 * topology.
 */
@Component(immediate = true)
public class PcepTopologyProvider extends AbstractProvider
        implements LinkProvider, DeviceProvider {

    /**
     * Creates instance of PCEP topology provider.
     */
    public PcepTopologyProvider() {
        //In BGP-PCEP app, since both BGP and PCEP topology provider have same scheme
        //so BGP will be primary and PCEP topology provider will be ancillary.
        super(new ProviderId("l3", "org.onosproject.provider.pcep", true));
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
    protected PcepClientController pcepClientController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netConfigRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService netConfigService;

    private DeviceProviderService deviceProviderService;
    private LinkProviderService linkProviderService;

    private HashMap<Long, List<PortDescription>> portMap = new HashMap<>();
    private InternalLinkProvider listener = new InternalLinkProvider();

    private final ConfigFactory<DeviceId, DeviceCapability> configFactory =
            new ConfigFactory<DeviceId, DeviceCapability>(SubjectFactories.DEVICE_SUBJECT_FACTORY,
                    DeviceCapability.class, "deviceCapability", false) {
                @Override
                public DeviceCapability createConfig() {
                    return new DeviceCapability();
                }
            };

    @Activate
    public void activate() {
        linkProviderService = linkProviderRegistry.register(this);
        deviceProviderService = deviceProviderRegistry.register(this);
        controller.addListener(listener);
        controller.addLinkListener(listener);
        pcepClientController.addNodeListener(listener);
        netConfigRegistry.registerConfigFactory(configFactory);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        linkProviderRegistry.unregister(this);
        linkProviderService = null;
        controller.removeListener(listener);
        controller.removeLinkListener(listener);
        pcepClientController.removeNodeListener(listener);
        netConfigRegistry.unregisterConfigFactory(configFactory);
        log.info("Stopped");
    }

    private List<PortDescription> buildPortDescriptions(PcepDpid dpid,
                                                        Port port) {

        List<PortDescription> portList;

        if (portMap.containsKey(dpid.value())) {
            portList = portMap.get(dpid.value());
        } else {
            portList = new ArrayList<>();
        }
        if (port != null) {
            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .putAll(port.annotations()).build();
            portList.add(new DefaultPortDescription(port.number(), port.isEnabled(),
                                                    port.type(), port.portSpeed(),
                                                    annotations));
        }

        portMap.put(dpid.value(), portList);
        return portList;
    }

    /**
     * Build a link description from a pcep link.
     *
     * @param pceLink pcep link
     * @return LinkDescription onos link description
     */
    private LinkDescription buildLinkDescription(PcepLink pceLink) {
        LinkDescription ld;
        checkNotNull(pceLink);
        DeviceId srcDeviceID = deviceId(uri(pceLink.linkSrcDeviceID()));
        DeviceId dstDeviceID = deviceId(uri(pceLink.linkDstDeviceId()));

        deviceProviderService
                .updatePorts(srcDeviceID,
                        buildPortDescriptions(pceLink.linkSrcDeviceID(),
                                pceLink.linkSrcPort()));

        deviceProviderService
                .updatePorts(dstDeviceID,
                        buildPortDescriptions(pceLink.linkDstDeviceId(),
                                pceLink.linkDstPort()));

        ConnectPoint src = new ConnectPoint(srcDeviceID, pceLink.linkSrcPort().number());

        ConnectPoint dst = new ConnectPoint(dstDeviceID, pceLink.linkDstPort().number());

        DefaultAnnotations extendedAttributes = DefaultAnnotations
                .builder()
                .set("subType", String.valueOf(pceLink.linkSubType()))
                .set("workState", pceLink.linkState())
                .set("distance", String.valueOf(pceLink.linkDistance()))
                .set("capType", pceLink.linkCapacityType().toLowerCase())
                .set("avail_" + pceLink.linkCapacityType().toLowerCase(),
                        String.valueOf(pceLink.linkAvailValue()))
                .set("max_" + pceLink.linkCapacityType().toLowerCase(),
                        String.valueOf(pceLink.linkMaxValue())).build();
        // construct the link
        ld = new DefaultLinkDescription(src, dst, Type.OPTICAL, extendedAttributes);
        return ld;
    }

    private class InternalLinkProvider
            implements PcepSwitchListener, PcepLinkListener, PcepNodeListener {

        @Override
        public void switchAdded(PcepDpid dpid) {
            if (deviceProviderService == null) {
                return;
            }
            DeviceId deviceId = deviceId(uri(dpid));
            PcepSwitch sw = controller.getSwitch(dpid);
            checkNotNull(sw, "device should not null.");
            // The default device type is switch.
            ChassisId cId = new ChassisId(dpid.value());
            Device.Type deviceType;

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
                    deviceId.uri(),
                    deviceType,
                    sw.manufacturerDescription(),
                    sw.hardwareDescription(),
                    sw.softwareDescription(),
                    sw.serialNumber(),
                    cId);
            deviceProviderService.deviceConnected(deviceId, description);

        }

        @Override
        public void switchRemoved(PcepDpid dpid) {
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
        public void handlePceplink(PcepLink link) {

            OperationType operType = link.getOperationType();
            LinkDescription ld = buildLinkDescription(link);
            if (ld == null) {
                log.error("Invalid link info.");
                return;
            }
            switch (operType) {
                case ADD:
                case UPDATE:
                    linkProviderService.linkDetected(ld);
                    break;

                case DELETE:
                    linkProviderService.linkVanished(ld);
                    break;

                default:
                    break;

            }
        }

        @Override
        public void addDevicePcepConfig(PcepClient pc) {
            if (netConfigRegistry == null) {
                log.error("Cannot add PCEP device capability as network config service is not available.");
                return;
            }
            DeviceId pccDeviceId = DeviceId.deviceId(String.valueOf(pc.getPccId().ipAddress()));
            DeviceCapability deviceCap = netConfigService.addConfig(pccDeviceId, DeviceCapability.class);
            deviceCap.setLabelStackCap(pc.capability().labelStackCapability())
                .setLocalLabelCap(pc.capability().pceccCapability())
                .setSrCap(pc.capability().srCapability())
                .apply();
        }

        @Override
        public void deleteDevicePcepConfig(PccId pccId) {
            if (netConfigRegistry == null) {
                log.error("Cannot remove PCEP device capability as network config service is not available.");
                return;
            }
            DeviceId pccDeviceId = DeviceId.deviceId(String.valueOf(pccId.ipAddress()));
            netConfigService.removeConfig(pccDeviceId, DeviceCapability.class);
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
        // TODO Auto-generated method stub
    }
}
