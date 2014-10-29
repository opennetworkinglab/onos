package org.onlab.onos.net.intent.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultEdgeLink;
import org.onlab.onos.net.DefaultPath;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.intent.PointToPointIntentWithBandwidthConstraint;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.resource.BandwidthResourceRequest;
import org.onlab.onos.net.resource.DefaultLinkResourceRequest;
import org.onlab.onos.net.resource.LinkResourceRequest;
import org.onlab.onos.net.resource.LinkResourceService;
import org.onlab.onos.net.resource.ResourceRequest;
import org.onlab.onos.net.resource.ResourceType;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyEdge;
import org.onlab.onos.net.topology.TopologyService;

/**
 * A intent compiler for {@link org.onlab.onos.net.intent.HostToHostIntent}.
 */
@Component(immediate = true)
public class PointToPointIntentWithBandwidthConstraintCompiler
        implements IntentCompiler<PointToPointIntentWithBandwidthConstraint> {

    private static final ProviderId PID = new ProviderId("core", "org.onlab.onos.core", true);
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService resourceService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(PointToPointIntentWithBandwidthConstraint.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PointToPointIntent.class);
    }

    @Override
    public List<Intent> compile(PointToPointIntentWithBandwidthConstraint intent) {
        Path path = getPath(intent.ingressPoint(), intent.egressPoint(), intent.bandwidthRequest());

        List<Link> links = new ArrayList<>();
        links.add(DefaultEdgeLink.createEdgeLink(intent.ingressPoint(), true));
        links.addAll(path.links());
        links.add(DefaultEdgeLink.createEdgeLink(intent.egressPoint(), false));

        return Arrays.asList(createPathIntent(new DefaultPath(PID, links, path.cost() + 2,
                                                              path.annotations()),
                                              intent));
    }

    /**
     * Creates a path intent from the specified path and original
     * connectivity intent.
     *
     * @param path   path to create an intent for
     * @param intent original intent
     */
    private Intent createPathIntent(Path path,
                                    PointToPointIntentWithBandwidthConstraint intent) {
        LinkResourceRequest.Builder request = DefaultLinkResourceRequest.builder(intent.id(),
                path.links())
                // TODO - this seems awkward, maybe allow directly attaching a BandwidthRequest
                .addBandwidthRequest(intent.bandwidthRequest().bandwidth().toDouble());
        LinkResourceRequest bandwidthRequest  = request.build();
        LinkResourceRequest[] bandwidthRequests = {bandwidthRequest};
        return new PathIntent(intent.appId(),
                              intent.selector(), intent.treatment(), path,
                              bandwidthRequests);
    }

    /**
     * Computes a path between two ConnectPoints.
     *
     * @param one start of the path
     * @param two end of the path
     * @return Path between the two
     * @throws org.onlab.onos.net.intent.impl.PathNotFoundException if a path cannot be found
     */
    private Path getPath(ConnectPoint one, ConnectPoint two, final BandwidthResourceRequest bandwidthRequest) {
        Topology topology = topologyService.currentTopology();
        LinkWeight weight = new LinkWeight() {
            @Override
            public double weight(TopologyEdge edge) {
                if (bandwidthRequest != null) {
                    double allocatedBandwidth = 0.0;
                    Iterable<ResourceRequest> availableResources = resourceService.getAvailableResources(edge.link());
                    for (ResourceRequest availableResource : availableResources) {
                        if (availableResource.type() == ResourceType.BANDWIDTH) {
                            BandwidthResourceRequest bandwidthRequest = (BandwidthResourceRequest) availableResource;
                            allocatedBandwidth += bandwidthRequest.bandwidth().toDouble();
                        }
                    }

                    // TODO this needs to be discovered from switch/ports somehow
                    double maxBandwidth = 1000;

                    double availableBandwidth = maxBandwidth - allocatedBandwidth;
                    if (availableBandwidth >= bandwidthRequest.bandwidth().toDouble()) {
                        return 1;
                    } else {
                        return -1;
                    }
                } else {
                    return 1;
                }
            }
        };

        Set<Path> paths = topologyService.getPaths(topology,
                one.deviceId(),
                two.deviceId(),
                weight);

        if (paths.isEmpty()) {
            throw new PathNotFoundException("No packet path from " + one + " to " + two);
        }
        // TODO: let's be more intelligent about this eventually
        return paths.iterator().next();
    }
}
