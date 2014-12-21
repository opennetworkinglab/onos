/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.sdnip.cli;

import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdnip.SdnIpService;
import org.onosproject.sdnip.bgp.BgpSession;

/**
 * Command to show the BGP neighbors.
 */
@Command(scope = "onos", name = "bgp-neighbors",
         description = "Lists the BGP neighbors")
public class BgpNeighborsListCommand extends AbstractShellCommand {
    @Option(name = "-n", aliases = "--neighbor",
            description = "BGP neighbor to display information about",
            required = false, multiValued = false)
    private String bgpNeighbor;

    private static final String FORMAT_NEIGHBOR_LINE1 =
        "BGP neighbor is %s, remote AS %d, local AS %d";
    private static final String FORMAT_NEIGHBOR_LINE2 =
        "  Remote router ID %s, IP %s, BGP version %d, Hold time %d";
    private static final String FORMAT_NEIGHBOR_LINE3 =
        "  Remote AFI/SAFI IPv4 Unicast %s Multicast %s, IPv6 Unicast %s Multicast %s";
    private static final String FORMAT_NEIGHBOR_LINE4 =
        "  Local  router ID %s, IP %s, BGP version %d, Hold time %d";
    private static final String FORMAT_NEIGHBOR_LINE5 =
        "  Local  AFI/SAFI IPv4 Unicast %s Multicast %s, IPv6 Unicast %s Multicast %s";
    private static final String FORMAT_NEIGHBOR_LINE6 =
        "  4 Octet AS Capability: %s %s";

    @Override
    protected void execute() {
        SdnIpService service = get(SdnIpService.class);
        Collection<BgpSession> bgpSessions = service.getBgpSessions();

        if (bgpNeighbor != null) {
            // Print a single neighbor (if found)
            BgpSession foundBgpSession = null;
            for (BgpSession bgpSession : bgpSessions) {
                if (bgpSession.getRemoteBgpId().toString().equals(bgpNeighbor)) {
                    foundBgpSession = bgpSession;
                    break;
                }
            }
            if (foundBgpSession != null) {
                printNeighbor(foundBgpSession);
            } else {
                print("BGP neighbor %s not found", bgpNeighbor);
            }
            return;
        }

        // Print all neighbors
        printNeighbors(bgpSessions);
    }

    /**
     * Prints all BGP neighbors.
     *
     * @param bgpSessions the BGP sessions for the neighbors to print
     */
    private void printNeighbors(Collection<BgpSession> bgpSessions) {
        if (outputJson()) {
            print("%s", json(bgpSessions));
        } else {
            for (BgpSession bgpSession : bgpSessions) {
                printNeighbor(bgpSession);
            }
        }
    }

    /**
     * Prints a BGP neighbor.
     *
     * @param bgpSession the BGP session for the neighbor to print
     */
    private void printNeighbor(BgpSession bgpSession) {
        print(FORMAT_NEIGHBOR_LINE1,
              bgpSession.getRemoteBgpId().toString(),
              bgpSession.getRemoteAs(),
              bgpSession.getLocalAs());
        print(FORMAT_NEIGHBOR_LINE2,
              bgpSession.getRemoteBgpId().toString(),
              bgpSession.getRemoteAddress().toString(),
              bgpSession.getRemoteBgpVersion(),
              bgpSession.getRemoteHoldtime());
        print(FORMAT_NEIGHBOR_LINE3,
              bgpSession.getRemoteIpv4Unicast() ? "YES" : "NO",
              bgpSession.getRemoteIpv4Multicast() ? "YES" : "NO",
              bgpSession.getRemoteIpv6Unicast() ? "YES" : "NO",
              bgpSession.getRemoteIpv6Multicast() ? "YES" : "NO");
        print(FORMAT_NEIGHBOR_LINE4,
              bgpSession.getLocalBgpId().toString(),
              bgpSession.getLocalAddress().toString(),
              bgpSession.getLocalBgpVersion(),
              bgpSession.getLocalHoldtime());
        print(FORMAT_NEIGHBOR_LINE5,
              bgpSession.getLocalIpv4Unicast() ? "YES" : "NO",
              bgpSession.getLocalIpv4Multicast() ? "YES" : "NO",
              bgpSession.getLocalIpv6Unicast() ? "YES" : "NO",
              bgpSession.getLocalIpv6Multicast() ? "YES" : "NO");
        if (bgpSession.getLocalAs4OctetCapability() || bgpSession.getRemoteAs4OctetCapability()) {
            print(FORMAT_NEIGHBOR_LINE6,
                  bgpSession.getLocalAs4OctetCapability() ? "Advertised" : "",
                  bgpSession.getRemoteAs4OctetCapability() ? "Received" : "");
        }
    }

    /**
     * Produces a JSON array of BGP neighbors.
     *
     * @param bgpSessions the BGP sessions with the data
     * @return JSON array with the neighbors
     */
    private JsonNode json(Collection<BgpSession> bgpSessions) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (BgpSession bgpSession : bgpSessions) {
            result.add(json(mapper, bgpSession));
        }
        return result;
    }

    /**
     * Produces JSON object for a BGP neighbor.
     *
     * @param mapper the JSON object mapper to use
     * @param bgpSession the BGP session with the data
     * @return JSON object for the route
     */
    private ObjectNode json(ObjectMapper mapper, BgpSession bgpSession) {
        ObjectNode result = mapper.createObjectNode();

        result.put("remoteAddress", bgpSession.getRemoteAddress().toString());
        result.put("remoteBgpVersion", bgpSession.getRemoteBgpVersion());
        result.put("remoteAs", bgpSession.getRemoteAs());
        result.put("remoteHoldtime", bgpSession.getRemoteHoldtime());
        result.put("remoteBgpId", bgpSession.getRemoteBgpId().toString());
        result.put("remoteIpv4Unicast", bgpSession.getRemoteIpv4Unicast());
        result.put("remoteIpv4Multicast", bgpSession.getRemoteIpv4Multicast());
        result.put("remoteIpv6Unicast", bgpSession.getRemoteIpv6Unicast());
        result.put("remoteIpv6Multicast", bgpSession.getRemoteIpv6Multicast());
        //
        result.put("localAddress", bgpSession.getLocalAddress().toString());
        result.put("localBgpVersion", bgpSession.getLocalBgpVersion());
        result.put("localAs", bgpSession.getLocalAs());
        result.put("localHoldtime", bgpSession.getLocalHoldtime());
        result.put("localBgpId", bgpSession.getLocalBgpId().toString());
        result.put("localIpv4Unicast", bgpSession.getLocalIpv4Unicast());
        result.put("localIpv4Multicast", bgpSession.getLocalIpv4Multicast());
        result.put("localIpv6Unicast", bgpSession.getLocalIpv6Unicast());
        result.put("localIpv6Multicast", bgpSession.getLocalIpv6Multicast());

        return result;
    }
}
