package org.onlab.onos.net.device.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.onos.net.device.DeviceEvent.Type.DEVICE_MASTERSHIP_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.MastershipEvent;
import org.onlab.onos.cluster.MastershipListener;
import org.onlab.onos.cluster.MastershipService;
import org.onlab.onos.cluster.MastershipTermService;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DeviceAdminService;
import org.onlab.onos.net.device.DeviceClockProviderService;
import org.onlab.onos.net.device.DeviceDescription;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceProvider;
import org.onlab.onos.net.device.DeviceProviderRegistry;
import org.onlab.onos.net.device.DeviceProviderService;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.device.DeviceStore;
import org.onlab.onos.net.device.DeviceStoreDelegate;
import org.onlab.onos.net.device.PortDescription;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

/**
 * Provides implementation of the device SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class DeviceManager
    extends AbstractProviderRegistry<DeviceProvider, DeviceProviderService>
    implements DeviceService, DeviceAdminService, DeviceProviderRegistry {

    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String PORT_NUMBER_NULL = "Port number cannot be null";
    private static final String DEVICE_DESCRIPTION_NULL = "Device description cannot be null";
    private static final String PORT_DESCRIPTION_NULL = "Port description cannot be null";
    private static final String ROLE_NULL = "Role cannot be null";

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<DeviceEvent, DeviceListener> listenerRegistry =
            new AbstractListenerRegistry<>();

    private final DeviceStoreDelegate delegate = new InternalStoreDelegate();

    private final MastershipListener mastershipListener = new InternalMastershipListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    protected MastershipTermService termService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceClockProviderService deviceClockProviderService;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(DeviceEvent.class, listenerRegistry);
        mastershipService.addListener(mastershipListener);
        termService = mastershipService.requestTermService();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        store.unsetDelegate(delegate);
        mastershipService.removeListener(mastershipListener);
        eventDispatcher.removeSink(DeviceEvent.class);
        log.info("Stopped");
    }

    @Override
    public int getDeviceCount() {
        return store.getDeviceCount();
    }

    @Override
    public Iterable<Device> getDevices() {
        return store.getDevices();
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getDevice(deviceId);
    }

    @Override
    public MastershipRole getRole(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return mastershipService.getLocalRole(deviceId);
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getPorts(deviceId);
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(portNumber, PORT_NUMBER_NULL);
        return store.getPort(deviceId, portNumber);
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.isAvailable(deviceId);
    }

    // Applies the specified role to the device; ignores NONE
    private void applyRole(DeviceId deviceId, MastershipRole newRole) {
        if (newRole.equals(MastershipRole.NONE)) {
            Device device = store.getDevice(deviceId);
            // FIXME: Device might not be there yet. (eventual consistent)
            if (device == null) {
                return;
            }
            DeviceProvider provider = getProvider(device.providerId());
            if (provider != null) {
                provider.roleChanged(device, newRole);
            }
            post(new DeviceEvent(DEVICE_MASTERSHIP_CHANGED, device));
        }
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        DeviceEvent event = store.removeDevice(deviceId);
        if (event != null) {
            log.info("Device {} administratively removed", deviceId);
            post(event);
        }
    }

    @Override
    public void addListener(DeviceListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(DeviceListener listener) {
        listenerRegistry.removeListener(listener);
    }

    @Override
    protected DeviceProviderService createProviderService(
            DeviceProvider provider) {
        return new InternalDeviceProviderService(provider);
    }

    // Personalized device provider service issued to the supplied provider.
    private class InternalDeviceProviderService
    extends AbstractProviderService<DeviceProvider>
    implements DeviceProviderService {

        InternalDeviceProviderService(DeviceProvider provider) {
            super(provider);
        }

        @Override
        public void deviceConnected(DeviceId deviceId,
                DeviceDescription deviceDescription) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(deviceDescription, DEVICE_DESCRIPTION_NULL);
            checkValidity();

            log.info("Device {} connected", deviceId);
            // check my Role
            MastershipRole role = mastershipService.requestRoleFor(deviceId);

            if (role != MastershipRole.MASTER) {
                // TODO: Do we need to explicitly tell the Provider that
                // this instance is no longer the MASTER? probably not
                return;
            }

            MastershipTerm term = mastershipService.requestTermService()
                    .getMastershipTerm(deviceId);
            if (!term.master().equals(clusterService.getLocalNode().id())) {
                // lost mastership after requestRole told this instance was MASTER.
                return;
            }
            // tell clock provider if this instance is the master
            deviceClockProviderService.setMastershipTerm(deviceId, term);

            DeviceEvent event = store.createOrUpdateDevice(provider().id(),
                    deviceId, deviceDescription);

            // If there was a change of any kind, tell the provider
            // that this instance is the master.
            // Note: event can be null, if mastership was lost between
            // roleRequest and store update calls.
            if (event != null) {
                // TODO: Check switch reconnected case. Is it assured that
                //       event will never be null?
                //       Could there be a situation MastershipService told this
                //       instance is the new Master, but
                //       event returned from the store is null?

                // TODO: Confirm: Mastership could be lost after requestRole
                //       and createOrUpdateDevice call.
                //       In that case STANDBY node can
                //       claim itself to be master against the Device.
                //       Will the Node, chosen by the MastershipService, retry
                //       to get the MASTER role when that happen?

                // FIXME: 1st argument should be deviceId, to allow setting
                //        certain roles even if the store returned null.
                provider().roleChanged(event.subject(), role);
                post(event);
            }
        }

        @Override
        public void deviceDisconnected(DeviceId deviceId) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkValidity();

            // FIXME: only the MASTER should be marking off-line in normal cases,
            // but if I was the last STANDBY connection, etc. and no one else
            // was there to mark the device offline, this instance may need to
            // temporarily request for Master Role and mark offline.
            if (!mastershipService.getLocalRole(deviceId).equals(MastershipRole.MASTER)) {
                log.debug("Device {} disconnected, but I am not the master", deviceId);
                //let go of ability to be backup
                mastershipService.relinquishMastership(deviceId);
                return;
            }
            DeviceEvent event = store.markOffline(deviceId);
            //relinquish master role and ability to be backup.
            mastershipService.relinquishMastership(deviceId);

            if (event != null) {
                log.info("Device {} disconnected", deviceId);
                post(event);
            }
        }

        @Override
        public void updatePorts(DeviceId deviceId,
                List<PortDescription> portDescriptions) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(portDescriptions,
                    "Port descriptions list cannot be null");
            checkValidity();
            this.provider().id();
            List<DeviceEvent> events = store.updatePorts(this.provider().id(),
                    deviceId, portDescriptions);
            for (DeviceEvent event : events) {
                post(event);
            }
        }

        @Override
        public void portStatusChanged(DeviceId deviceId,
                PortDescription portDescription) {
            checkNotNull(deviceId, DEVICE_ID_NULL);
            checkNotNull(portDescription, PORT_DESCRIPTION_NULL);
            checkValidity();
            DeviceEvent event = store.updatePortStatus(this.provider().id(),
                    deviceId, portDescription);
            if (event != null) {
                log.info("Device {} port {} status changed", deviceId, event
                        .port().number());
                post(event);
            }
        }

        @Override
        public void unableToAssertRole(DeviceId deviceId, MastershipRole role) {
            // FIXME: implement response to this notification
            log.warn("Failed to assert role [{}] onto Device {}", role,
                    deviceId);
            if (role == MastershipRole.MASTER) {
                mastershipService.relinquishMastership(deviceId);
            }
        }
    }

    // Posts the specified event to the local event dispatcher.
    private void post(DeviceEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

    // Intercepts mastership events
    private class InternalMastershipListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            final DeviceId did = event.subject();
            final NodeId myNodeId = clusterService.getLocalNode().id();

            if (myNodeId.equals(event.master())) {
                MastershipTerm term = termService.getMastershipTerm(did);

                if (term.master().equals(myNodeId)) {
                    // only set the new term if I am the master
                    deviceClockProviderService.setMastershipTerm(did, term);
                }

                // FIXME: we should check that the device is connected on our end.
                // currently, this is not straight forward as the actual switch
                // implementation is hidden from the registry.
                if (!isAvailable(did)) {
                    //flag the device as online. Is there a better way to do this?
                    Device device = getDevice(did);
                    store.createOrUpdateDevice(device.providerId(), did,
                            new DefaultDeviceDescription(
                                    did.uri(), device.type(), device.manufacturer(),
                                    device.hwVersion(), device.swVersion(),
                                    device.serialNumber()));
                }

                applyRole(did, MastershipRole.MASTER);
            } else {
                applyRole(did, MastershipRole.STANDBY);
            }
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate
    implements DeviceStoreDelegate {
        @Override
        public void notify(DeviceEvent event) {
            post(event);
        }
    }
}
