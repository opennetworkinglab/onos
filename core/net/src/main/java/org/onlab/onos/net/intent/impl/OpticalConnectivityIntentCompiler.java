package org.onlab.onos.net.intent.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.OpticalConnectivityIntent;
import org.onlab.onos.net.intent.OpticalPathIntent;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.resource.LinkResourceService;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.PathService;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyEdge;
import org.onlab.onos.net.topology.TopologyService;
import org.slf4j.Logger;

/**
 * Optical compiler for OpticalConnectivityIntent.
 * It firstly computes K-shortest paths in the optical-layer, then choose the optimal one to assign a wavelength.
 * Finally, it generates one or more opticalPathintent(s) with opticalMatchs and opticalActions.
 */
@Component(immediate = true)
public class OpticalConnectivityIntentCompiler implements IntentCompiler<OpticalConnectivityIntent> {

    private final Logger log = getLogger(getClass());
    private static final ProviderId PID = new ProviderId("core", "org.onlab.onos.core", true);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        intentManager.registerCompiler(OpticalConnectivityIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalConnectivityIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalConnectivityIntent intent) {
        // TODO: compute multiple paths using the K-shortest path algorithm
        List<Intent> retList = new ArrayList<>();
        log.info("The system is comipling the OpticalConnectivityIntent:" + intent.toString());
        Path path = calculateOpticalPath(intent.getSrcConnectPoint(), intent.getDst());
        if (path == null) {
            return retList;
        } else {
            log.info("the computed lightpath is : {}.", path.toString());
        }

        List<Link> links = new ArrayList<>();
        // links.add(DefaultEdgeLink.createEdgeLink(intent.getSrcConnectPoint(), true));
        links.addAll(path.links());
        //links.add(DefaultEdgeLink.createEdgeLink(intent.getDst(), false));

        // create a new opticalPathIntent
        Intent newIntent = new OpticalPathIntent(intent.appId(),
                intent.getSrcConnectPoint(),
                intent.getDst(),
                path);

        log.info("a new OpticalPathIntent was created: " + newIntent.toString());

        retList.add(newIntent);

        return retList;
    }

    private Path calculateOpticalPath(ConnectPoint start, ConnectPoint end) {
        // TODO: support user policies
        Topology topology = topologyService.currentTopology();
        LinkWeight weight = new LinkWeight() {
            @Override
            public double weight(TopologyEdge edge) {
                Link.Type lt = edge.link().type();
                if (lt == Link.Type.OPTICAL) {
                    return 1.0;
                } else {
                    return 1000.0;
                }
            }
        };

        Set<Path> paths = topologyService.getPaths(topology,
                start.deviceId(),
                end.deviceId(),
                weight);

        ArrayList<Path> localPaths = new ArrayList<>();
        Iterator<Path> itr = paths.iterator();
        while (itr.hasNext()) {
            Path path = itr.next();
            if (path.cost() >= 1000) {
                continue;
            }
            localPaths.add(path);
        }

        if (localPaths.isEmpty()) {
            throw new PathNotFoundException("No fiber path from " + start + " to " + end);
        } else {
            return localPaths.iterator().next();
        }

    }

}
