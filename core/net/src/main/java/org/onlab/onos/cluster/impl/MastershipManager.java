package org.onlab.onos.cluster.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.cluster.MastershipAdminService;
import org.onlab.onos.cluster.MastershipEvent;
import org.onlab.onos.cluster.MastershipListener;
import org.onlab.onos.cluster.MastershipProvider;
import org.onlab.onos.cluster.MastershipProviderService;
import org.onlab.onos.cluster.MastershipService;
import org.onlab.onos.cluster.MastershipStore;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MastershipManager
        extends AbstractProviderRegistry<MastershipProvider, MastershipProviderService>
        implements MastershipService, MastershipAdminService {

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String ROLE_NULL = "Mastership role cannot be null";

    private final Logger log = getLogger(getClass());

    protected final AbstractListenerRegistry<MastershipEvent, MastershipListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterManager clusterManager;

    @Activate
    public void activate() {
        eventDispatcher.addSink(MastershipEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(MastershipEvent.class);
        log.info("Stopped");
    }

    @Override
    public void setRole(NodeId nodeId, DeviceId deviceId, MastershipRole role) {
        checkNotNull(nodeId, NODE_ID_NULL);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(role, ROLE_NULL);
        store.setRole(nodeId, deviceId, role);
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
    public MastershipRole requestRoleFor(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        NodeId id = clusterManager.getLocalNode().id();
        return store.getRole(id, deviceId);
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

    @Override
    protected MastershipProviderService createProviderService(
            MastershipProvider provider) {
        return new InternalMastershipProviderService(provider);
    }

    private class InternalMastershipProviderService
            extends AbstractProviderService<MastershipProvider>
            implements MastershipProviderService {

        protected InternalMastershipProviderService(MastershipProvider provider) {
            super(provider);
        }

        @Override
        public void roleChanged(NodeId nodeId, DeviceId deviceId, MastershipRole role) {
            // TODO Auto-generated method stub
            MastershipEvent event =
                    store.addOrUpdateDevice(nodeId, deviceId, role);
            post(event);
        }
    }

    // Posts the specified event to the local event dispatcher.
    private void post(MastershipEvent event) {
        if (event != null && eventDispatcher != null) {
            eventDispatcher.post(event);
        }
    }

}
