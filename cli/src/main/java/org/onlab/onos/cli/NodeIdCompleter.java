package org.onlab.onos.cli;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Node ID completer.
 */
public class NodeIdCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        ClusterService service = AbstractShellCommand.get(ClusterService.class);
        Iterator<ControllerNode> it = service.getNodes().iterator();
        SortedSet<String> strings = delegate.getStrings();
        while (it.hasNext()) {
            strings.add(it.next().id().toString());
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
