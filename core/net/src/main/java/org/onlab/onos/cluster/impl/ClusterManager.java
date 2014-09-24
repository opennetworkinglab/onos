package org.onlab.onos.cluster.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ClusterAdminService;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ClusterStore;
import org.onlab.onos.cluster.ClusterStoreDelegate;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.slf4j.Logger;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the cluster service.
 */
@Component(immediate = true)
@Service
public class ClusterManager implements ClusterService, ClusterAdminService {

    public static final String INSTANCE_ID_NULL = "Instance ID cannot be null";
    private final Logger log = getLogger(getClass());

    private ClusterStoreDelegate delegate = new InternalStoreDelegate();

    protected final AbstractListenerRegistry<ClusterEvent, ClusterEventListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Activate
    public void activate() {
        store.setDelegate(delegate);
        eventDispatcher.addSink(ClusterEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(ClusterEvent.class);
        log.info("Stopped");
    }

    @Override
    public ControllerNode getLocalNode() {
        return store.getLocalNode();
    }

    @Override
    public Set<ControllerNode> getNodes() {
        return store.getNodes();
    }

    @Override
    public ControllerNode getNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return store.getNode(nodeId);
    }

    @Override
    public ControllerNode.State getState(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        return store.getState(nodeId);
    }

    @Override
    public void removeNode(NodeId nodeId) {
        checkNotNull(nodeId, INSTANCE_ID_NULL);
        store.removeNode(nodeId);
    }

    @Override
    public void addListener(ClusterEventListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(ClusterEventListener listener) {
        listenerRegistry.removeListener(listener);
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements ClusterStoreDelegate {
        @Override
        public void notify(ClusterEvent event) {
            checkNotNull(event, "Event cannot be null");
            eventDispatcher.post(event);
        }
    }
}
