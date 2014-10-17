package org.onlab.onos.provider.of.device.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
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
import org.onlab.packet.ChassisId;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.onlab.onos.net.DeviceId.deviceId;
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
    public void triggerProbe(Device device) {
        LOG.info("Triggering probe on device {}", device.id());
    }

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
            ChassisId cId = new ChassisId(dpid.value());
            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(), Device.Type.SWITCH,
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
        private List<PortDescription> buildPortDescriptions(
                List<OFPortDesc> ports) {
            final List<PortDescription> portDescs = new ArrayList<>();
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
            final PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
            final boolean enabled = !port.getState().contains(OFPortState.LINK_DOWN) &&
                    !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            return new DefaultPortDescription(portNo, enabled);
        }

    }

}
