package org.onlab.onos.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.cli.Comparators;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.host.HostService;
import org.onlab.packet.IpPrefix;

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
        if (outputJson()) {
            print("%s", json(getSortedHosts(service)));
        } else {
            for (Host host : getSortedHosts(service)) {
                printHost(host);
            }
        }
    }

    // Produces JSON structure.
    private static JsonNode json(Iterable<Host> hosts) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Host host : hosts) {
            result.add(json(mapper, host));
        }
        return result;
    }

    // Produces JSON structure.
    private static JsonNode json(ObjectMapper mapper, Host host) {
        ObjectNode loc = LinksListCommand.json(mapper, host.location())
                .put("time", host.location().time());
        ArrayNode ips = mapper.createArrayNode();
        for (IpPrefix ip : host.ipAddresses()) {
            ips.add(ip.toString());
        }
        ObjectNode result = mapper.createObjectNode()
                .put("id", host.id().toString())
                .put("mac", host.mac().toString())
                .put("vlan", host.vlan().toString());
        result.set("location", loc);
        result.set("ips", ips);
        return result;
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
     * @param host end-station host
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
