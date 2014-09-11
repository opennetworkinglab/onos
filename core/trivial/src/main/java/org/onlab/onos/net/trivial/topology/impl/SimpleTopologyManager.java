package org.onlab.onos.net.trivial.topology.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.Event;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.provider.AbstractProviderRegistry;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.onlab.onos.net.topology.ClusterId;
import org.onlab.onos.net.topology.GraphDescription;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyEvent;
import org.onlab.onos.net.topology.TopologyGraph;
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
    private static final String CLUSTER_ID_NULL = "Cluster ID cannot be null";
    private static final String CLUSTER_NULL = "Topology cluster cannot be null";
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
        return store.isLatest(defaultTopology(topology));
    }

    // Validates the specified topology and returns it as a default
    private DefaultTopology defaultTopology(Topology topology) {
        if (topology instanceof DefaultTopology) {
            return (DefaultTopology) topology;
        }
        throw new IllegalArgumentException("Topology class " + topology.getClass() +
                                                   " not supported");
    }

    @Override
    public Set<TopologyCluster> getClusters(Topology topology) {
        checkNotNull(topology, TOPOLOGY_NULL);
        return store.getClusters(defaultTopology(topology));
    }

    @Override
    public TopologyCluster getCluster(Topology topology, ClusterId clusterId) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(topology, CLUSTER_ID_NULL);
        return store.getCluster(defaultTopology(topology), clusterId);
    }

    @Override
    public Set<DeviceId> getClusterDevices(Topology topology, TopologyCluster cluster) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(topology, CLUSTER_NULL);
        return store.getClusterDevices(defaultTopology(topology), cluster);
    }

    @Override
    public Set<Link> getClusterLinks(Topology topology, TopologyCluster cluster) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(topology, CLUSTER_NULL);
        return store.getClusterLinks(defaultTopology(topology), cluster);
    }

    @Override
    public TopologyGraph getGraph(Topology topology) {
        checkNotNull(topology, TOPOLOGY_NULL);
        return store.getGraph(defaultTopology(topology));
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        return store.getPaths(defaultTopology(topology), src, dst);
    }

    @Override
    public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(src, DEVICE_ID_NULL);
        checkNotNull(dst, DEVICE_ID_NULL);
        checkNotNull(weight, "Link weight cannot be null");
        return store.getPaths(defaultTopology(topology), src, dst, weight);
    }

    @Override
    public boolean isInfrastructure(Topology topology, ConnectPoint connectPoint) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(connectPoint, CONNECTION_POINT_NULL);
        return store.isInfrastructure(defaultTopology(topology), connectPoint);
    }

    @Override
    public boolean isInBroadcastTree(Topology topology, ConnectPoint connectPoint) {
        checkNotNull(topology, TOPOLOGY_NULL);
        checkNotNull(connectPoint, CONNECTION_POINT_NULL);
        return store.isInBroadcastTree(defaultTopology(topology), connectPoint);
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
        public void topologyChanged(GraphDescription topoDescription,
                                    List<Event> reasons) {
            checkNotNull(topoDescription, "Topology description cannot be null");

            TopologyEvent event = store.updateTopology(provider().id(),
                                                       topoDescription, reasons);
            if (event != null) {
                log.info("Topology {} changed due to: {}", event.subject(),
                         reasons == null ? "initial compute" : reasons);
                eventDispatcher.post(event);
            }
        }
    }

}
