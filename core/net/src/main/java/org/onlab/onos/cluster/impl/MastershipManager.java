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
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.MastershipAdminService;
import org.onlab.onos.cluster.MastershipEvent;
import org.onlab.onos.cluster.MastershipListener;
import org.onlab.onos.cluster.MastershipService;
import org.onlab.onos.cluster.MastershipStore;
import org.onlab.onos.cluster.MastershipStoreDelegate;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.MastershipTermService;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.slf4j.Logger;

@Component(immediate = true)
@Service
public class MastershipManager
implements MastershipService, MastershipAdminService {

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String ROLE_NULL = "Mastership role cannot be null";

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<MastershipEvent, MastershipListener>
    listenerRegistry = new AbstractListenerRegistry<>();

    private final MastershipStoreDelegate delegate = new InternalDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Activate
    public void activate() {
        eventDispatcher.addSink(MastershipEvent.class, listenerRegistry);
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(MastershipEvent.class);
        store.unsetDelegate(delegate);
        log.info("Stopped");
    }

    @Override
    public void setRole(NodeId nodeId, DeviceId deviceId, MastershipRole role) {
        checkNotNull(nodeId, NODE_ID_NULL);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(role, ROLE_NULL);
        //TODO figure out appropriate action for non-MASTER roles, if we even set those
        if (role.equals(MastershipRole.MASTER)) {
            MastershipEvent event = store.setMaster(nodeId, deviceId);
            if (event != null) {
                post(event);
            }
        }
    }

    @Override
    public MastershipRole getLocalRole(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getRole(clusterService.getLocalNode().id(), deviceId);
    }

    @Override
    public void relinquishMastership(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        // FIXME: add method to store to give up mastership and trigger new master selection process
    }

    @Override
    public MastershipRole requestRoleFor(DeviceId deviceId) {
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
    public MastershipTermService requestTermService() {
        return new InternalMastershipTermService();
    }

    @Override
    public void addListener(MastershipListener listener) {
        checkNotNull(listener);
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(MastershipListener listener) {
        checkNotNull(listener);
        listenerRegistry.removeListener(listener);
    }

    // FIXME: provide wiring to allow events to be triggered by changes within the store

    // Posts the specified event to the local event dispatcher.
    private void post(MastershipEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

    private class InternalMastershipTermService implements MastershipTermService {

        @Override
        public MastershipTerm getMastershipTerm(DeviceId deviceId) {
            return store.getTermFor(deviceId);
        }

    }

    public class InternalDelegate implements MastershipStoreDelegate {

        @Override
        public void notify(MastershipEvent event) {
            log.info("dispatching mastership event {}", event);
            eventDispatcher.post(event);
        }

    }

}
