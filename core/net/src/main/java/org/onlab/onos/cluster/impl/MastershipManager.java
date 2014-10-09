package org.onlab.onos.cluster.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceMastershipAdminService;
import org.onlab.onos.net.device.DeviceMastershipEvent;
import org.onlab.onos.net.device.DeviceMastershipListener;
import org.onlab.onos.net.device.DeviceMastershipRole;
import org.onlab.onos.net.device.DeviceMastershipService;
import org.onlab.onos.net.device.DeviceMastershipStore;
import org.onlab.onos.net.device.DeviceMastershipStoreDelegate;
import org.onlab.onos.net.device.DeviceMastershipTerm;
import org.onlab.onos.net.device.DeviceMastershipTermService;
import org.slf4j.Logger;

@Component(immediate = true)
@Service
public class MastershipManager
implements DeviceMastershipService, DeviceMastershipAdminService {

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String ROLE_NULL = "Mastership role cannot be null";

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<DeviceMastershipEvent, DeviceMastershipListener>
    listenerRegistry = new AbstractListenerRegistry<>();

    private final DeviceMastershipStoreDelegate delegate = new InternalDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceMastershipStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private ClusterEventListener clusterListener = new InternalClusterEventListener();

    @Activate
    public void activate() {
        eventDispatcher.addSink(DeviceMastershipEvent.class, listenerRegistry);
        clusterService.addListener(clusterListener);
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(DeviceMastershipEvent.class);
        clusterService.removeListener(clusterListener);
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void setRole(NodeId nodeId, DeviceId deviceId, DeviceMastershipRole role) {
        checkNotNull(nodeId, NODE_ID_NULL);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(role, ROLE_NULL);

        DeviceMastershipEvent event = null;
        if (role.equals(DeviceMastershipRole.MASTER)) {
            event = store.setMaster(nodeId, deviceId);
        } else {
            event = store.setStandby(nodeId, deviceId);
        }

        if (event != null) {
            post(event);
        }
    }

    @Override
    public DeviceMastershipRole getLocalRole(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getRole(clusterService.getLocalNode().id(), deviceId);
    }

    @Override
    public void relinquishMastership(DeviceId deviceId) {
        DeviceMastershipEvent event = null;
        event = store.relinquishRole(
                clusterService.getLocalNode().id(), deviceId);

        if (event != null) {
            post(event);
        }
    }

    @Override
    public DeviceMastershipRole requestRoleFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.requestRole(deviceId);
    }

    @Override
    public NodeId getMasterFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getMaster(deviceId);
    }

    @Override
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        checkNotNull(nodeId, NODE_ID_NULL);
        return store.getDevices(nodeId);
    }


    @Override
    public DeviceMastershipTermService requestTermService() {
        return new InternalMastershipTermService();
    }

    @Override
    public void addListener(DeviceMastershipListener listener) {
        checkNotNull(listener);
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(DeviceMastershipListener listener) {
        checkNotNull(listener);
        listenerRegistry.removeListener(listener);
    }

    // FIXME: provide wiring to allow events to be triggered by changes within the store

    // Posts the specified event to the local event dispatcher.
    private void post(DeviceMastershipEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

    private class InternalMastershipTermService implements DeviceMastershipTermService {

        @Override
        public DeviceMastershipTerm getMastershipTerm(DeviceId deviceId) {
            return store.getTermFor(deviceId);
        }

    }

    //callback for reacting to cluster events
    private class InternalClusterEventListener implements ClusterEventListener {

        @Override
        public void event(ClusterEvent event) {
            switch (event.type()) {
                //FIXME: worry about addition when the time comes
                case INSTANCE_ADDED:
                case INSTANCE_ACTIVATED:
                     break;
                case INSTANCE_REMOVED:
                case INSTANCE_DEACTIVATED:
                    break;
                default:
                    log.warn("unknown cluster event {}", event);
            }
        }

    }

    public class InternalDelegate implements DeviceMastershipStoreDelegate {

        @Override
        public void notify(DeviceMastershipEvent event) {
            log.info("dispatching mastership event {}", event);
            eventDispatcher.post(event);
        }

    }

}
