package org.onlab.onos.net.trivial.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.graph.Graph;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.TopoEdge;
import org.onlab.onos.net.topology.TopoVertex;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyDescription;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyListener;
import org.onlab.onos.net.topology.TopologyProvider;
import org.onlab.onos.net.topology.TopologyProviderRegistry;
import org.onlab.onos.net.topology.TopologyProviderService;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides basic implementation of the topology SB &amp; NB APIs.
 */
@Component(immediate = true)
@Service
public class SimpleTopologyManager
        extends AbstractProviderRegistry<TopologyProvider, TopologyProviderService>
        implements TopologyService, TopologyProviderRegistry {

    public static final String TOPOLOGY_NULL = "Topology cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    public static final String CONNECTION_POINT_NULL = "Connection point cannot be null";

    private final Logger log = getLogger(getClass());

    private final AbstractListenerRegistry<TopologyEvent, TopologyListener>
            listenerRegistry = new AbstractListenerRegistry<>();

    private final SimpleTopologyStore store = new SimpleTopologyStore();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private EventDeliveryService eventDispatcher;


    @Activate
    public void activate() {
        eventDispatcher.addSink(TopologyEvent.class, listenerRegistry);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(TopologyEvent.class);
        log.info("Stopped");
    }

    @Override
    public Topology currentTopology() {
        return store.currentTopology();
    }

    @Override
    public boolean isLatest(Topology topology) {
        checkNotNull(topology, TOPOLOGY_NULL);
        return store.isLatest(topology);
    }

    @Override
    public Set<TopologyCluster> getClusters(Topology topology) {
        checkNotNull(topology, TOPOLOGY_NULL);
        return store.getClusters(topology);
    }

    @Override
    public Graph<TopoVertex, TopoEdge> getGraph(Topology topology) {
        checkNotNull(topology, TOPOLOGY_NULL);
        return store.getGraph(topology);
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        return store.getPaths(topology, src, dst);
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        checkNotNull(weight, "Link weight cannot be null");
        return store.getPaths(topology, src, dst, weight);
    }

    @Override
    public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(connectPoint, CONNECTION_POINT_NULL);
        return store.isInfrastructure(topology, connectPoint);
    }

    @Override
    public boolean isInBroadcastTree(Topology topology, ConnectPoint connectPoint) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(connectPoint, CONNECTION_POINT_NULL);
        return store.isInBroadcastTree(topology, connectPoint);
    }

    @Override
    public void addListener(TopologyListener listener) {
        listenerRegistry.addListener(listener);
    }

    @Override
    public void removeListener(TopologyListener listener) {
        listenerRegistry.removeListener(listener);
    }

    // Personalized host provider service issued to the supplied provider.
    @Override
    protected TopologyProviderService createProviderService(TopologyProvider provider) {
        return new InternalTopologyProviderService(provider);
    }

    private class InternalTopologyProviderService
            extends AbstractProviderService<TopologyProvider>
            implements TopologyProviderService {

        InternalTopologyProviderService(TopologyProvider provider) {
            super(provider);
        }

        @Override
        public void topologyChanged(TopologyDescription topoDescription,
                                    List<Event> reasons) {
            checkNotNull(topoDescription, "Topology description cannot be null");
            TopologyEvent event = store.updateTopology(topoDescription, reasons);
            if (event != null) {
                log.info("Topology changed due to: {}",
                         reasons == null ? "initial compute" : reasons);
                eventDispatcher.post(event);
            }
        }
    }

}
