package org.onlab.onos.net.intent.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.IdGenerator;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.topology.PathService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.onlab.onos.net.flow.DefaultTrafficSelector.builder;

/**
 * A intent compiler for {@link HostToHostIntent}.
 */
@Component(immediate = true)
public class HostToHostIntentCompiler
        implements IntentCompiler<HostToHostIntent> {

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
        intentManager.registerCompiler(HostToHostIntent.class, this);
    }

    @Deactivate
    public void deactivate() {
        intentManager.unregisterCompiler(HostToHostIntent.class);
    }

    @Override
    public List<Intent> compile(HostToHostIntent intent) {
        Path pathOne = getPath(intent.one(), intent.two());
        Path pathTwo = getPath(intent.two(), intent.one());

        Host one = hostService.getHost(intent.one());
        Host two = hostService.getHost(intent.two());

        return Arrays.asList(createPathIntent(pathOne, one, two, intent),
                             createPathIntent(pathTwo, two, one, intent));
    }

    // Creates a path intent from the specified path and original connectivity intent.
    private Intent createPathIntent(Path path, Host src, Host dst,
                                    HostToHostIntent intent) {

        TrafficSelector selector = builder(intent.selector())
                .matchEthSrc(src.mac()).matchEthDst(dst.mac()).build();

        return new PathIntent(intentIdGenerator.getNewId(),
                              selector, intent.treatment(),
                              path.src(), path.dst(), path);
    }

    private Path getPath(HostId one, HostId two) {
        Set<Path> paths = pathService.getPaths(one, two);
        if (paths.isEmpty()) {
            throw new PathNotFoundException("No path from host " + one + " to " + two);
        }
        // TODO: let's be more intelligent about this eventually
        return paths.iterator().next();
    }
}
