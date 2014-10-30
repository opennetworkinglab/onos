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
package org.onlab.onos.provider.of.device.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DefaultPortDescription;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderRegistry;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.onos.openflow.controller.OpenFlowController;
import org.onlab.onos.openflow.controller.OpenFlowSwitch;
import org.onlab.onos.openflow.controller.OpenFlowSwitchListener;
import org.onlab.onos.openflow.controller.RoleState;
import org.onlab.onos.openflow.controller.driver.OpenFlowSwitchDriver;
import org.onlab.packet.ChassisId;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortFeatures;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.PortSpeed;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.Port.Type.COPPER;
import static org.onlab.onos.net.Port.Type.FIBER;
import static org.onlab.onos.openflow.controller.Dpid.dpid;
import static org.onlab.onos.openflow.controller.Dpid.uri;
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
        super(new ProviderId("of", "org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        for (OpenFlowSwitch sw : controller.getSwitches()) {
            listener.switchAdded(new Dpid(sw.getId()));
        }
        LOG.info("Started");
    }

    @Deactivate
    public void deactivate() {
        for (OpenFlowSwitch sw : controller.getSwitches()) {
            providerService.deviceDisconnected(DeviceId.deviceId(uri(sw.getId())));
        }
        providerRegistry.unregister(this);
        controller.removeListener(listener);
        providerService = null;

        LOG.info("Stopped");
    }


    @Override
    public boolean isReachable(Device device) {
        // FIXME if possible, we might want this to be part of
        // OpenFlowSwitch interface so the driver interface isn't misused.
        OpenFlowSwitch sw = controller.getSwitch(dpid(device.id().uri()));
        if (sw == null || !((OpenFlowSwitchDriver) sw).isConnected()) {
            return false;
        }
        return true;
        //return checkChannel(device, sw);
    }

    @Override
    public void triggerProbe(Device device) {
        LOG.info("Triggering probe on device {}", device.id());

        OpenFlowSwitch sw = controller.getSwitch(dpid(device.id().uri()));
        //if (!checkChannel(device, sw)) {
        //  LOG.error("Failed to probe device {} on sw={}", device, sw);
        //  providerService.deviceDisconnected(device.id());
        //return;
        //}

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

    // Checks if the OF channel is connected.
    //private boolean checkChannel(Device device, OpenFlowSwitch sw) {
    // FIXME if possible, we might want this to be part of
    // OpenFlowSwitch interface so the driver interface isn't misused.
    //    if (sw == null || !((OpenFlowSwitchDriver) sw).isConnected()) {
    //      return false;
    //      }
    //    return true;
    // }

    @Override
    public void roleChanged(Device device, MastershipRole newRole) {
        switch (newRole) {
            case MASTER:
                controller.setRole(dpid(device.id().uri()), RoleState.MASTER);
                break;
            case STANDBY:
                controller.setRole(dpid(device.id().uri()), RoleState.EQUAL);
                break;
            case NONE:
                controller.setRole(dpid(device.id().uri()), RoleState.SLAVE);
                break;
            default:
                LOG.error("Unknown Mastership state : {}", newRole);

        }
        LOG.info("Accepting mastership role change for device {}", device.id());
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
            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(), deviceType,
                                                 sw.manfacturerDescription(),
                                                 sw.hardwareDescription(),
                                                 sw.softwareDescription(),
                                                 sw.serialNumber(),
                                                 cId);
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
            PortDescription portDescription = buildPortDescription(status.getDesc());
            providerService.portStatusChanged(deviceId(uri(dpid)), portDescription);
        }

        @Override
        public void roleAssertFailed(Dpid dpid, RoleState role) {
            MastershipRole failed;
            switch (role) {
                case MASTER:
                    failed = MastershipRole.MASTER;
                    break;
                case EQUAL:
                    failed = MastershipRole.STANDBY;
                    break;
                case SLAVE:
                    failed = MastershipRole.NONE;
                    break;
                default:
                    LOG.warn("unknown role {}", role);
                    return;
            }
            providerService.unableToAssertRole(deviceId(uri(dpid)), failed);
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
            return new DefaultPortDescription(portNo, enabled, type, portSpeed(port));
        }

        private long portSpeed(OFPortDesc port) {
            if (port.getVersion() == OFVersion.OF_13) {
                return port.getCurrSpeed() / MBPS;
            }

            PortSpeed portSpeed = PortSpeed.SPEED_NONE;
            for (OFPortFeatures feat : port.getCurr()) {
                portSpeed = PortSpeed.max(portSpeed, feat.getPortSpeed());
            }
            return portSpeed.getSpeedBps();
        }
    }

}
