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
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.IdGenerator;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.PathService;

/**
 * A intent compiler for {@link org.onlab.onos.net.intent.HostToHostIntent}.
 */
@Component(immediate = true)
public class PointToPointIntentCompiler
        implements IntentCompiler<PointToPointIntent> {

    private static final ProviderId PID = new ProviderId("core", "org.onlab.onos.core", true);
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private IdGenerator<IntentId> intentIdGenerator;

    @Activate
    public void activate() {
        IdBlockAllocator idBlockAllocator = new DummyIdBlockAllocator();
        intentIdGenerator = new IdBlockAllocatorBasedIntentIdGenerator(idBlockAllocator);
        intentManager.registerCompiler(PointToPointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PointToPointIntent.class);
    }

    @Override
    public List<Intent> compile(PointToPointIntent intent) {
        Path path = getPath(intent.ingressPoint(), intent.egressPoint());

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
     * @param path path to create an intent for
     * @param intent original intent
     */
    private Intent createPathIntent(Path path,
                                    PointToPointIntent intent) {

        return new PathIntent(intentIdGenerator.getNewId(),
                              intent.selector(), intent.treatment(),
                              path.src(), path.dst(), path);
    }

    /**
     * Computes a path between two ConnectPoints.
     *
     * @param one start of the path
     * @param two end of the path
     * @return Path between the two
     * @throws PathNotFoundException if a path cannot be found
     */
    private Path getPath(ConnectPoint one, ConnectPoint two) {
        Set<Path> paths = pathService.getPaths(one.deviceId(), two.deviceId());
        if (paths.isEmpty()) {
            throw new PathNotFoundException("No path from " + one + " to " + two);
        }
        // TODO: let's be more intelligent about this eventually
        return paths.iterator().next();
    }
}
