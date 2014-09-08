package org.onlab.onos.cli.net;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.device.DeviceService;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

/**
 * Device ID completer.
 */
public class DeviceIdCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        DeviceService service = AbstractShellCommand.get(DeviceService.class);
        Iterator<Device> it = service.getDevices().iterator();
        SortedSet<String> strings = delegate.getStrings();
        while (it.hasNext()) {
            strings.add(it.next().id().toString());
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
