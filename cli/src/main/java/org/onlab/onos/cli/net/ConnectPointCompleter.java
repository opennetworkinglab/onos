package org.onlab.onos.cli.net;

import java.util.List;
import java.util.SortedSet;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.device.DeviceService;

/**
 * ConnectPoint completer.
 */
public class ConnectPointCompleter implements Completer {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        DeviceService service = AbstractShellCommand.get(DeviceService.class);

        // Generate the device ID/port number identifiers
        for (Device device : service.getDevices()) {
            SortedSet<String> strings = delegate.getStrings();

            for (Port port : service.getPorts(device.id())) {
                strings.add(device.id().toString() + "/" + port.number());
            }
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
