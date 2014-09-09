package org.onlab.onos.cli.net;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.net.MastershipRole;

import java.util.List;
import java.util.SortedSet;

/**
 * Device mastership role completer.
 */
public class RoleCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();
        SortedSet<String> strings = delegate.getStrings();
        strings.add(MastershipRole.MASTER.toString().toLowerCase());
        strings.add(MastershipRole.STANDBY.toString().toLowerCase());
        strings.add(MastershipRole.NONE.toString().toLowerCase());

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
