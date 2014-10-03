package org.onlab.onos.net.intent.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.intent.HostToHostIntent;
import org.onlab.onos.net.intent.IdGenerator;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentCompiler;
import org.onlab.onos.net.intent.IntentExtensionService;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.PathIntent;
import org.onlab.onos.net.topology.PathService;

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
        Set<Path> paths = pathService.getPaths(intent.getSrc(), intent.getDst());
        if (paths.isEmpty()) {
            throw new PathNotFoundException();
        }
        Path path = paths.iterator().next();

        return Arrays.asList((Intent) new PathIntent(
                intentIdGenerator.getNewId(),
                intent.getTrafficSelector(),
                intent.getTrafficTreatment(),
                new ConnectPoint(intent.getSrc(), PortNumber.ALL),
                new ConnectPoint(intent.getDst(), PortNumber.ALL),
                path));
    }
}
