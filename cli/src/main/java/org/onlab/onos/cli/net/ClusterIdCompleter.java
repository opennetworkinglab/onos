package org.onlab.onos.cli.net;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyService;

import java.util.List;
import java.util.SortedSet;

/**
 * Cluster ID completer.
 */
public class ClusterIdCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        TopologyService service = AbstractShellCommand.get(TopologyService.class);
        Topology topology = service.currentTopology();

        SortedSet<String> strings = delegate.getStrings();
        for (TopologyCluster cluster : service.getClusters(topology)) {
            strings.add(Integer.toString(cluster.id().index()));
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
