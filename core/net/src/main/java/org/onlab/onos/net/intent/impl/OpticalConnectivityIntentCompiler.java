package org.onlab.onos.net.intent.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;

import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultEdgeLink;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import org.onlab.onos.net.intent.IdGenerator;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.OpticalConnectivityIntent;
import org.onlab.onos.net.intent.OpticalPathIntent;

import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.PathService;
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

    // protected LinkResourceService resourceService;

    protected IdGenerator<IntentId> intentIdGenerator;

    @Activate
    public void activate() {
        IdBlockAllocator idBlockAllocator = new DummyIdBlockAllocator();
        intentIdGenerator = new IdBlockAllocatorBasedIntentIdGenerator(idBlockAllocator);
        intentManager.registerCompiler(OpticalConnectivityIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(OpticalConnectivityIntent.class);
    }

    @Override
    public List<Intent> compile(OpticalConnectivityIntent intent) {
        // TO DO: compute multiple paths using the K-shortest path algorithm
        Path path = calculatePath(intent.getSrcConnectPoint(), intent.getDst());
        log.info("calculate the lightpath: {}.", path.toString());

        List<Link> links = new ArrayList<>();
        links.add(DefaultEdgeLink.createEdgeLink(intent.getSrcConnectPoint(), true));
        links.addAll(path.links());
        links.add(DefaultEdgeLink.createEdgeLink(intent.getDst(), false));

        // TO DO: choose a wavelength using the first-fit algorithm
        TrafficSelector opticalSelector = null;
        TrafficTreatment opticalTreatment = null;

        List<Intent> retList = new ArrayList<>();
        int wavelength = assignWavelength(path);
        log.info("assign the wavelength: {}.", wavelength);

        // create a new opticalPathIntent
        Intent newIntent = new OpticalPathIntent(intentIdGenerator.getNewId(),
                opticalSelector,
                opticalTreatment,
                path.src(),
                path.dst(),
                path);

        retList.add(newIntent);

        return retList;
    }

    private Path calculatePath(ConnectPoint one, ConnectPoint two) {
        // TODO: K-shortest path computation algorithm
        Set<Path> paths = pathService.getPaths(one.deviceId(), two.deviceId());
        if (paths.isEmpty()) {
            throw new PathNotFoundException("No optical path from " + one + " to " + two);
        }
        return paths.iterator().next();
    }

    private int assignWavelength(Path path) {
        // TODO: wavelength assignment
        return 1;
    }

}
