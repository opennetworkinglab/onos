package org.onlab.onos.cli;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.GreetService;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Simple example of a command-line parameter completer.
 * For a more open-ended sets a more efficient implementation would be required.
 */
public class NameCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        GreetService greetService = AbstractShellCommand.get(GreetService.class);
        Iterator<String> it = greetService.names().iterator();
        SortedSet<String> strings = delegate.getStrings();
        while (it.hasNext()) {
            strings.add(it.next());
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
