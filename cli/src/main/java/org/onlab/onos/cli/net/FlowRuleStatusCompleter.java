package org.onlab.onos.cli.net;

import java.util.List;
import java.util.SortedSet;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.net.flow.FlowEntry.FlowEntryState;

/**
 * Device ID completer.
 */
public class FlowRuleStatusCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        FlowEntryState[] states = FlowEntryState.values();
        SortedSet<String> strings = delegate.getStrings();
        for (int i = 0; i < states.length; i++) {
            strings.add(states[i].toString().toLowerCase());
        }
        strings.add(FlowsListCommand.ANY);

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
