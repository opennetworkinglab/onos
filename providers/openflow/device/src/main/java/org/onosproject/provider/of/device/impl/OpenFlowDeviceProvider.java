/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.provider.of.device.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.RoleState;
import org.onlab.packet.ChassisId;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortFeatures;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.PortSpeed;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.Port.Type.FIBER;
import static org.onosproject.openflow.controller.Dpid.dpid;
import static org.onosproject.openflow.controller.Dpid.uri;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure devices.
 */
@Component(immediate = true)
public class OpenFlowDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final Logger LOG = getLogger(OpenFlowDeviceProvider.class);
    private static final long MBPS = 1_000 * 1_000;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private DeviceProviderService providerService;

    private final OpenFlowSwitchListener listener = new InternalDeviceProvider();

    /**
     * Creates an OpenFlow device provider.
     */
    public OpenFlowDeviceProvider() {
        super(new ProviderId("of", "org.onosproject.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        for (OpenFlowSwitch sw : controller.getSwitches()) {
            try {
                listener.switchAdded(new Dpid(sw.getId()));
            } catch (Exception e) {
                LOG.warn("Failed initially adding {} : {}", sw.getStringId(), e.getMessage());
                LOG.debug("Error details:", e);
                // disconnect to trigger switch-add later
                sw.disconnectSwitch();
            }
        }
        LOG.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        controller.removeListener(listener);
        providerService = null;

        LOG.info("Stopped");
    }


    @Override
    public boolean isReachable(DeviceId deviceId) {
        OpenFlowSwitch sw = controller.getSwitch(dpid(deviceId.uri()));
        if (sw == null || !sw.isConnected()) {
            return false;
        }
        return true;
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        LOG.info("Triggering probe on device {}", deviceId);

        final Dpid dpid = dpid(deviceId.uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null || !sw.isConnected()) {
            LOG.error("Failed to probe device {} on sw={}", deviceId, sw);
            providerService.deviceDisconnected(deviceId);
        } else {
            LOG.trace("Confirmed device {} connection", deviceId);
        }

        // Prompt an update of port information. We can use any XID for this.
        OFFactory fact = sw.factory();
        switch (fact.getVersion()) {
            case OF_10:
                sw.sendMsg(fact.buildFeaturesRequest().setXid(0).build());
                break;
            case OF_13:
                sw.sendMsg(fact.buildPortDescStatsRequest().setXid(0).build());
                break;
            default:
                LOG.warn("Unhandled protocol version");
        }
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        switch (newRole) {
            case MASTER:
                controller.setRole(dpid(deviceId.uri()), RoleState.MASTER);
                break;
            case STANDBY:
                controller.setRole(dpid(deviceId.uri()), RoleState.EQUAL);
                break;
            case NONE:
                controller.setRole(dpid(deviceId.uri()), RoleState.SLAVE);
                break;
            default:
                LOG.error("Unknown Mastership state : {}", newRole);

        }
        LOG.info("Accepting mastership role change for device {}", deviceId);
    }

    private class InternalDeviceProvider implements OpenFlowSwitchListener {
        @Override
        public void switchAdded(Dpid dpid) {
            if (providerService == null) {
                return;
            }
            DeviceId did = deviceId(uri(dpid));
            OpenFlowSwitch sw = controller.getSwitch(dpid);

            Device.Type deviceType = sw.isOptical() ? Device.Type.ROADM :
                    Device.Type.SWITCH;
            ChassisId cId = new ChassisId(dpid.value());

            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set("protocol", sw.factory().getVersion().toString())
                    .set("channelId", sw.channelId())
                    .build();

            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(), deviceType,
                                                 sw.manufacturerDescription(),
                                                 sw.hardwareDescription(),
                                                 sw.softwareDescription(),
                                                 sw.serialNumber(),
                                                 cId, annotations);
            providerService.deviceConnected(did, description);
            providerService.updatePorts(did, buildPortDescriptions(sw.getPorts()));
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            if (providerService == null) {
                return;
            }
            providerService.deviceDisconnected(deviceId(uri(dpid)));
        }


        @Override
        public void switchChanged(Dpid dpid) {
            if (providerService == null) {
                return;
            }
            DeviceId did = deviceId(uri(dpid));
            OpenFlowSwitch sw = controller.getSwitch(dpid);
            providerService.updatePorts(did, buildPortDescriptions(sw.getPorts()));
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            PortDescription portDescription = buildPortDescription(status);
            providerService.portStatusChanged(deviceId(uri(dpid)), portDescription);
        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {
            MastershipRole request = roleOf(requested);
            MastershipRole reply = roleOf(response);

            providerService.receivedRoleReply(deviceId(uri(dpid)), request, reply);
        }

        /**
         * Translates a RoleState to the corresponding MastershipRole.
         *
         * @param response
         * @return a MastershipRole
         */
        private MastershipRole roleOf(RoleState response) {
            switch (response) {
                case MASTER:
                    return MastershipRole.MASTER;
                case EQUAL:
                    return MastershipRole.STANDBY;
                case SLAVE:
                    return MastershipRole.NONE;
                default:
                    LOG.warn("unknown role {}", response);
                    return null;
            }
        }

        /**
         * Builds a list of port descriptions for a given list of ports.
         *
         * @param ports the list of ports
         * @return list of portdescriptions
         */
        private List<PortDescription> buildPortDescriptions(List<OFPortDesc> ports) {
            final List<PortDescription> portDescs = new ArrayList<>(ports.size());
            for (OFPortDesc port : ports) {
                portDescs.add(buildPortDescription(port));
            }
            return portDescs;
        }

        /**
         * Creates an annotation for the port name if one is available.
         *
         * @param port description of the port
         * @return annotation containing the port name if one is found,
         *         null otherwise
         */
        private SparseAnnotations makePortNameAnnotation(OFPortDesc port) {
            SparseAnnotations annotations = null;
            String portName = Strings.emptyToNull(port.getName());
            if (portName != null) {
                annotations = DefaultAnnotations.builder()
                        .set("portName", portName).build();
            }
            return annotations;
        }

        /**
         * Build a portDescription from a given port.
         *
         * @param port the port to build from.
         * @return portDescription for the port.
         */
        private PortDescription buildPortDescription(OFPortDesc port) {
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
            boolean enabled =
                    !port.getState().contains(OFPortState.LINK_DOWN) &&
                            !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            Port.Type type = port.getCurr().contains(OFPortFeatures.PF_FIBER) ? FIBER : COPPER;
            SparseAnnotations annotations = makePortNameAnnotation(port);
            return new DefaultPortDescription(portNo, enabled, type,
                                              portSpeed(port), annotations);
        }

        private PortDescription buildPortDescription(OFPortStatus status) {
            OFPortDesc port = status.getDesc();
            if (status.getReason() != OFPortReason.DELETE) {
                return buildPortDescription(port);
            } else {
                PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
                Port.Type type = port.getCurr().contains(OFPortFeatures.PF_FIBER) ? FIBER : COPPER;
                SparseAnnotations annotations = makePortNameAnnotation(port);
                return new DefaultPortDescription(portNo, false, type,
                                                  portSpeed(port), annotations);
            }
        }

        private long portSpeed(OFPortDesc port) {
            if (port.getVersion() == OFVersion.OF_13) {
                return port.getCurrSpeed() / MBPS;
            }

            PortSpeed portSpeed = PortSpeed.SPEED_NONE;
            for (OFPortFeatures feat : port.getCurr()) {
                portSpeed = PortSpeed.max(portSpeed, feat.getPortSpeed());
            }
            return portSpeed.getSpeedBps() / MBPS;
        }
    }

}
