/*
 * Copyright 2014-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.utils.Comparators;

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
            "id=%s, mac=%s, location=%s/%s, vlan=%s, ip(s)=%s%s";

    private static final String FMT_SHORT =
            "id=%s, mac=%s, location=%s/%s, vlan=%s, ip(s)=%s";

    @Option(name = "-s", aliases = "--short", description = "Show short output only",
            required = false, multiValued = false)
    private boolean shortOnly = false;

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
    private JsonNode json(Iterable<Host> hosts) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        hosts.forEach(host -> result.add(jsonForEntity(host, Host.class)));
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
        if (shortOnly) {
            print(FMT_SHORT, host.id(), host.mac(),
                  host.location().deviceId(), host.location().port(),
                  host.vlan(), host.ipAddresses());
        } else {
            print(FMT, host.id(), host.mac(),
                  host.location().deviceId(), host.location().port(),
                  host.vlan(), host.ipAddresses(), annotations(host.annotations()));
        }
    }
}
