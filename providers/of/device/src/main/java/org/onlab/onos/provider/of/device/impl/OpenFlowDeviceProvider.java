package org.onlab.onos.provider.of.device.impl;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Device;
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
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowController;
import org.onlab.onos.of.controller.OpenFlowSwitch;
import org.onlab.onos.of.controller.OpenFlowSwitchListener;
import org.onlab.onos.of.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.slf4j.Logger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure devices.
 */
@Component(immediate = true)
public class OpenFlowDeviceProvider extends AbstractProvider implements DeviceProvider {

    private final Logger log = getLogger(getClass());

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
        super(new ProviderId("org.onlab.onos.provider.of.device"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        controller.removeListener(listener);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Device device) {
        log.info("Triggering probe on device {}", device.id());
    }

    @Override
    public void roleChanged(Device device, MastershipRole newRole) {
        switch (newRole) {
        case MASTER:
            controller.setRole(new Dpid(device.id().uri().getSchemeSpecificPart()),
                    RoleState.MASTER);
            break;
        case STANDBY:
            controller.setRole(new Dpid(device.id().uri().getSchemeSpecificPart()),
                    RoleState.EQUAL);
        case NONE:
            controller.setRole(new Dpid(device.id().uri().getSchemeSpecificPart()),
                    RoleState.SLAVE);
            break;
        default:
            log.error("Unknown Mastership state : {}", newRole);

        }
        log.info("Accepting mastership role change for device {}", device.id());
    }

    private class InternalDeviceProvider implements OpenFlowSwitchListener {
        @Override
        public void switchAdded(Dpid dpid) {
            if (providerService == null) {
                return;
            }
            URI uri = buildURI(dpid);
            OpenFlowSwitch sw = controller.getSwitch(dpid);

            DeviceDescription description =
                    new DefaultDeviceDescription(buildURI(dpid), Device.Type.SWITCH,
                            sw.manfacturerDescription(),
                            sw.hardwareDescription(),
                            sw.softwareDescription(),
                            sw.serialNumber());
            providerService.deviceConnected(deviceId(uri), description);
            providerService.updatePorts(deviceId(uri), buildPortDescriptions(sw.getPorts()));
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            if (providerService == null) {
                return;
            }
            URI uri = buildURI(dpid);
            providerService.deviceDisconnected(deviceId(uri));
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            final PortDescription portDescription = buildPortDescription(status.getDesc());
            final URI uri = buildURI(dpid);
            providerService.portStatusChanged(deviceId(uri), portDescription);
        }

        /**
         * Given a dpid builds a URI for the device.
         * @param dpid the dpid to build the uri from
         * @return returns a uri of the form of:<dpidHexForm>
         */
        private URI buildURI(Dpid dpid) {
            URI uri = null;
            try {
                uri = new URI("of", Long.toHexString(dpid.value()), null);
            } catch (URISyntaxException e) {
                log.warn("URI construction for device {} failed.", dpid);
            }
            return uri;
        }

        /**
         * Builds a list of port descriptions for a given list of ports.
         * @param ports the list of ports
         * @return list of portdescriptions
         */
        private List<PortDescription> buildPortDescriptions(
                List<OFPortDesc> ports) {
            final List<PortDescription> portDescs = new ArrayList<PortDescription>();
            for (OFPortDesc port : ports) {
                portDescs.add(buildPortDescription(port));
            }
            return portDescs;
        }

        /**
         * Build a portDescription from a given port.
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
