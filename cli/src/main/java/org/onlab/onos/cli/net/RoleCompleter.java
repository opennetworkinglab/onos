package org.onlab.onos.cli.net;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.net.device.DeviceMastershipRole;

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
        strings.add(DeviceMastershipRole.MASTER.toString().toLowerCase());
        strings.add(DeviceMastershipRole.STANDBY.toString().toLowerCase());
        strings.add(DeviceMastershipRole.NONE.toString().toLowerCase());

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
