package org.onlab.onos.cli.net;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentService;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Intent ID completer.
 */
public class IntentIdCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        IntentService service = AbstractShellCommand.get(IntentService.class);
        Iterator<Intent> it = service.getIntents().iterator();
        SortedSet<String> strings = delegate.getStrings();
        while (it.hasNext()) {
            strings.add(it.next().id().toString());
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
