package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.host.HostService;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists all currently-known hosts.
 */
@Command(scope = "onos", name = "hosts",
        description = "Lists all currently-known hosts.")
public class HostsListCommand extends AbstractShellCommand {

    private static final String FMT =
            "id=%s, mac=%s, location=%s/%s, vlan=%s, ip(s)=%s";

    @Override
    protected void execute() {
        HostService service = get(HostService.class);
        for (Host host : getSortedHosts(service)) {
            printHost(host);
        }
    }

    /**
     * Returns the list of devices sorted using the device ID URIs.
     *
     * @param service device service
     * @return sorted device list
     */
    protected List<Host> getSortedHosts(HostService service) {
        List<Host> hosts = newArrayList(service.getHosts());
        Collections.sort(hosts, Comparators.ELEMENT_COMPARATOR);
        return hosts;
    }

    /**
     * Prints information about a host.
     *
     * @param host
     */
    protected void printHost(Host host) {
        if (host != null) {
            print(FMT, host.id(), host.mac(),
                    host.location().deviceId(),
                    host.location().port(),
                    host.vlan(), host.ipAddresses());
        }
    }
 }
