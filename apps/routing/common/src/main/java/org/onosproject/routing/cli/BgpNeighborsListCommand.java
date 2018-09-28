/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.routing.bgp.BgpInfoService;
import org.onosproject.routing.bgp.BgpSession;

import java.util.Collection;

/**
 * Command to show the BGP neighbors.
 */
@Service
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
    protected void doExecute() {
        BgpInfoService service = AbstractShellCommand.get(BgpInfoService.class);
        Collection<BgpSession> bgpSessions = service.getBgpSessions();

        if (bgpNeighbor != null) {
            // Print a single neighbor (if found)
            BgpSession foundBgpSession = null;
            for (BgpSession bgpSession : bgpSessions) {
                if (bgpSession.remoteInfo().bgpId().toString().equals(bgpNeighbor)) {
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
              bgpSession.remoteInfo().bgpId().toString(),
              bgpSession.remoteInfo().asNumber(),
              bgpSession.localInfo().asNumber());
        print(FORMAT_NEIGHBOR_LINE2,
              bgpSession.remoteInfo().bgpId().toString(),
              bgpSession.remoteInfo().address().toString(),
              bgpSession.remoteInfo().bgpVersion(),
              bgpSession.remoteInfo().holdtime());
        print(FORMAT_NEIGHBOR_LINE3,
              bgpSession.remoteInfo().ipv4Unicast() ? "YES" : "NO",
              bgpSession.remoteInfo().ipv4Multicast() ? "YES" : "NO",
              bgpSession.remoteInfo().ipv6Unicast() ? "YES" : "NO",
              bgpSession.remoteInfo().ipv6Multicast() ? "YES" : "NO");
        print(FORMAT_NEIGHBOR_LINE4,
              bgpSession.localInfo().bgpId().toString(),
              bgpSession.localInfo().address().toString(),
              bgpSession.localInfo().bgpVersion(),
              bgpSession.localInfo().holdtime());
        print(FORMAT_NEIGHBOR_LINE5,
              bgpSession.localInfo().ipv4Unicast() ? "YES" : "NO",
              bgpSession.localInfo().ipv4Multicast() ? "YES" : "NO",
              bgpSession.localInfo().ipv6Unicast() ? "YES" : "NO",
              bgpSession.localInfo().ipv6Multicast() ? "YES" : "NO");
        if (bgpSession.localInfo().as4OctetCapability() ||
            bgpSession.remoteInfo().as4OctetCapability()) {
            print(FORMAT_NEIGHBOR_LINE6,
                  bgpSession.localInfo().as4OctetCapability() ? "Advertised" : "",
                  bgpSession.remoteInfo().as4OctetCapability() ? "Received" : "");
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

        result.put("remoteAddress", bgpSession.remoteInfo().address().toString());
        result.put("remoteBgpVersion", bgpSession.remoteInfo().bgpVersion());
        result.put("remoteAs", bgpSession.remoteInfo().asNumber());
        result.put("remoteAs4", bgpSession.remoteInfo().as4Number());
        result.put("remoteHoldtime", bgpSession.remoteInfo().holdtime());
        result.put("remoteBgpId", bgpSession.remoteInfo().bgpId().toString());
        result.put("remoteIpv4Unicast", bgpSession.remoteInfo().ipv4Unicast());
        result.put("remoteIpv4Multicast", bgpSession.remoteInfo().ipv4Multicast());
        result.put("remoteIpv6Unicast", bgpSession.remoteInfo().ipv6Unicast());
        result.put("remoteIpv6Multicast", bgpSession.remoteInfo().ipv6Multicast());
        //
        result.put("localAddress", bgpSession.localInfo().address().toString());
        result.put("localBgpVersion", bgpSession.localInfo().bgpVersion());
        result.put("localAs", bgpSession.localInfo().asNumber());
        result.put("localAs4", bgpSession.localInfo().as4Number());
        result.put("localHoldtime", bgpSession.localInfo().holdtime());
        result.put("localBgpId", bgpSession.localInfo().bgpId().toString());
        result.put("localIpv4Unicast", bgpSession.localInfo().ipv4Unicast());
        result.put("localIpv4Multicast", bgpSession.localInfo().ipv4Multicast());
        result.put("localIpv6Unicast", bgpSession.localInfo().ipv6Unicast());
        result.put("localIpv6Multicast", bgpSession.localInfo().ipv6Multicast());

        return result;
    }
}
