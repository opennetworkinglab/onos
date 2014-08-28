package org.onlab.onos.net.trivial.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractListenerManager;
import org.onlab.onos.event.EventDispatchService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderBroker;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.AbstractProviderBroker;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides basic implementation of the device SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class SimpleDeviceManager
        extends AbstractProviderBroker<DeviceProvider, DeviceProviderService>
        implements DeviceService, DeviceProviderBroker {

    public static final String DEVICE_ID_NULL = "Device ID cannot be null";
    public static final String PORT_NUMBER_NULL = "Port number cannot be null";
    public static final String DEVICE_DESCRIPTION_NULL = "Device description cannot be null";
    public static final String PORT_DESCRIPTION_NULL = "Port description cannot be null";

    private Logger log = LoggerFactory.getLogger(SimpleDeviceManager.class);

    private final AbstractListenerManager<DeviceEvent, DeviceListener>
            listenerManager = new AbstractListenerManager<>();

    private EventDispatchService eventDispatcher;

    private final DeviceStore store = new DeviceStore();

    @Activate
    public void activate() {
//        eventDispatcher.addSink(DeviceEvent.class, listenerManager);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
//        eventDispatcher.removeSink(DeviceEvent.class);
        log.info("Stopped");
    }

    @Override
    public MastershipRole getRole(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return null;
    }

    @Override
    public Iterable<Device> getDevices() {
        return null;
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return null;
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return null;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(portNumber, PORT_NUMBER_NULL);
        return null;
    }

    @Override
    public void addListener(DeviceListener listener) {
        listenerManager.addListener(listener);
    }

    @Override
    public void removeListener(DeviceListener listener) {
        listenerManager.removeListener(listener);
    }

    @Override
    protected DeviceProviderService createProviderService(DeviceProvider provider) {
        return new InternalDeviceProviderService(provider);
    }

    // Personalized device provider service issued to the supplied provider.
    private class InternalDeviceProviderService extends AbstractProviderService<DeviceProvider>
            implements DeviceProviderService {

        public InternalDeviceProviderService(DeviceProvider provider) {
            super(provider);
        }

        @Override
        public MastershipRole deviceConnected(DeviceId deviceId, DeviceDescription deviceDescription) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(deviceDescription, DEVICE_DESCRIPTION_NULL);
            log.info("Device {} connected: {}", deviceId, deviceDescription);
            DeviceEvent event = store.createOrUpdateDevice(deviceId, deviceDescription);
            post(event);
            return MastershipRole.MASTER;
        }

        @Override
        public void deviceDisconnected(DeviceId deviceId) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            log.info("Device {} disconnected", deviceId);
            DeviceEvent event = store.removeDevice(deviceId);
            post(event);
        }

        @Override
        public void updatePorts(DeviceId deviceId, List<PortDescription> portDescriptions) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(portDescriptions, "Port descriptions list cannot be null");
            // FIXME: fix the interface to accept DeviceId separately
            log.info("Device {} ports updated: {}", portDescriptions);
            List<DeviceEvent> events = store.updatePorts(deviceId, portDescriptions);
            for (DeviceEvent event : events) {
                post(event);
            }
        }

        @Override
        public void portStatusChanged(DeviceId deviceId, PortDescription portDescription) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(portDescription, PORT_DESCRIPTION_NULL);
            log.info("Device {} port status changed: {}", deviceId, portDescription);
            DeviceEvent event = store.updatePortStatus(deviceId, portDescription);
            post(event);
        }
    }

    // Posts the specified event to a local event dispatcher
    private void post(DeviceEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

}
