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
import org.onlab.onos.CoreService;
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
        Path path = calculatePath(intent.getSrcConnectPoint(), intent.getDst());
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

    private Path calculatePath(ConnectPoint start, ConnectPoint end) {
        // TODO: support user policies
        Topology topology = topologyService.currentTopology();
        LinkWeight weight = new LinkWeight() {
            @Override
            public double weight(TopologyEdge edge) {
                boolean isOptical = false;

                Link.Type lt = edge.link().type();

                //String t = edge.link().annotations().value("linkType");
                if (lt == Link.Type.OPTICAL) {
                   isOptical = true;
                }
                if (isOptical) {
                    return 1; // optical links
                } else {
                    return 10000; // packet links
                }
            }
        };

        Set<Path> paths = topologyService.getPaths(topology,
                start.deviceId(),
                end.deviceId(),
                weight);

        Iterator<Path> itr = paths.iterator();
        while (itr.hasNext()) {
            Path path = itr.next();
            // log.info(String.format("total link number.:%d", path.links().size()));
            if (path.cost() >= 10000) {
                itr.remove();
            }
        }

        if (paths.isEmpty()) {
            log.info("No optical path found from " + start + " to " + end);
            return null;
        } else {
            return paths.iterator().next();
        }

    }

}
