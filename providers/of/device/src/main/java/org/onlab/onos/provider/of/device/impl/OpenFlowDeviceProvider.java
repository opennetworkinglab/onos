package org.onlab.onos.provider.of.device.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.device.DeviceProviderBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Device provider which uses an OpenFlow controller to detect devices.
 */
@Component
public class OpenFlowDeviceProvider {

    private final Logger log = LoggerFactory.getLogger(OpenFlowDeviceProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderBroker broker;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected OpenFlowController controller;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

}
