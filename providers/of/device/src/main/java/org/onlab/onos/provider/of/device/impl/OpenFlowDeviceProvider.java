package org.onlab.onos.provider.of.device.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderBroker;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure devices.
 */
@Component(immediate = true)
public class OpenFlowDeviceProvider extends AbstractProvider implements DeviceProvider {

    private final Logger log = LoggerFactory.getLogger(OpenFlowDeviceProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderBroker providerBroker;

    private DeviceProviderService providerService;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected OpenFlowController controller;


    /**
     * Creates an OpenFlow device provider.
     */
    public OpenFlowDeviceProvider() {
        super(new ProviderId("org.onlab.onos.provider.of.device"));
    }

    @Activate
    public void activate() {
        providerService = providerBroker.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerBroker.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Device device) {
        log.info("Triggering probe on device {}", device.id());
    }

    @Override
    public void roleChanged(Device device, MastershipRole newRole) {
        log.info("Accepting mastership role change for device {}", device.id());
    }

}
