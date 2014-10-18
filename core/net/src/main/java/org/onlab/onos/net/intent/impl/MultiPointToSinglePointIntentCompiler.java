package org.onlab.onos.net.intent.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.intent.IdGenerator;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.LinkCollectionIntent;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.net.intent.PointToPointIntent;
import org.onlab.onos.net.topology.PathService;

/**
 * An intent compiler for
 * {@link org.onlab.onos.net.intent.MultiPointToSinglePointIntent}.
 */
@Component(immediate = true)
public class MultiPointToSinglePointIntentCompiler
        implements IntentCompiler<MultiPointToSinglePointIntent> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentExtensionService intentManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    protected IdGenerator<IntentId> intentIdGenerator;

    @Activate
    public void activate() {
        IdBlockAllocator idBlockAllocator = new DummyIdBlockAllocator();
        intentIdGenerator = new IdBlockAllocatorBasedIntentIdGenerator(idBlockAllocator);
        intentManager.registerCompiler(MultiPointToSinglePointIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(PointToPointIntent.class);
    }

    @Override
    public List<Intent> compile(MultiPointToSinglePointIntent intent) {
        Set<Link> links = new HashSet<>();

        for (ConnectPoint ingressPoint : intent.ingressPoints()) {
            Path path = getPath(ingressPoint, intent.egressPoint());
            links.addAll(path.links());
        }

        Intent result = new LinkCollectionIntent(intentIdGenerator.getNewId(),
                intent.selector(), intent.treatment(),
                links, intent.egressPoint());
        return Arrays.asList(result);
    }

    /**
     * Computes a path between two ConnectPoints.
     *
     * @param one start of the path
     * @param two end of the path
     * @return Path between the two
     * @throws org.onlab.onos.net.intent.impl.PathNotFoundException if a path cannot be found
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
