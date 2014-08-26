package org.onlab.onos.net.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderBroker;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.AbstractProviderBroker;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Provides basic implementation of the device SB & NB APIs.
 */
@Component(immediate = true)
@Service
public class SimpleDeviceManager implements DeviceProviderBroker {

    private Logger log = LoggerFactory.getLogger(SimpleDeviceManager.class);

    private final DeviceProviderBroker broker = new InternalBroker();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public DeviceProviderService register(DeviceProvider provider) {
        return broker.register(provider);
    }

    @Override
    public void unregister(DeviceProvider provider) {
        broker.unregister(provider);
    }

    // Internal delegate for tracking various providers and issuing them a
    // personalized provider service.
    private class InternalBroker extends AbstractProviderBroker<DeviceProvider, DeviceProviderService>
            implements DeviceProviderBroker {
        @Override
        protected DeviceProviderService createProviderService(DeviceProvider provider) {
            return new InternalDeviceProviderService(provider);
        }
    }

    // Personalized device provider service issued to the supplied provider.
    private class InternalDeviceProviderService extends AbstractProviderService<DeviceProvider>
            implements DeviceProviderService {

        public InternalDeviceProviderService(DeviceProvider provider) {
            super(provider);
        }

        @Override
        public MastershipRole deviceConnected(DeviceId deviceId, DeviceDescription deviceDescription) {
            log.info("Device {} connected: {}", deviceId, deviceDescription);
            return MastershipRole.MASTER;
        }

        @Override
        public void deviceDisconnected(DeviceId deviceId) {
            log.info("Device {} disconnected", deviceId);

        }

        @Override
        public void updatePorts(DeviceId deviceId, List<PortDescription> ports) {
            // FIXME: fix the interface to accept DeviceId separately
            log.info("Device {} ports updated: {}", ports);

        }

        @Override
        public void portStatusChanged(DeviceId deviceId, PortDescription port) {
            log.info("Device {} port status changed: {}", deviceId, port);
        }
    }
}
