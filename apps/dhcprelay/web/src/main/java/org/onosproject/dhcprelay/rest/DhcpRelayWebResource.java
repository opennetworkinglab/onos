/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */

package org.onosproject.dhcprelay.rest;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.Tools;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.dhcprelay.api.DhcpRelayService;
import org.onosproject.dhcprelay.api.DhcpServerInfo;
import org.onosproject.dhcprelay.cli.DhcpRelayCommand;
import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.dhcprelay.store.DhcpRelayCounters;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteStore;
import org.onosproject.routing.fpm.api.FpmRecord;
import org.slf4j.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * DHCP Relay agent REST API.
 */
@Path("fpm-delete")
public class DhcpRelayWebResource extends AbstractWebResource {
    private static final Logger LOG = getLogger(DhcpRelayWebResource.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final DhcpRelayService dhcpDelayService = get(DhcpRelayService.class);
    private static final String NA = "N/A";
    List<DhcpServerInfo> defaultDhcpServerInfoList = dhcpDelayService.getDefaultDhcpServerInfoList();
    List<DhcpServerInfo> indirectDhcpServerInfoList = dhcpDelayService.getIndirectDhcpServerInfoList();
    Collection<DhcpRecord> records = dhcpDelayService.getDhcpRecords();


    /**
     * Deletes the fpm route from fpm record.
     * Corresponding route from the route store
     *
     * @param prefix IpPrefix
     * @return 204 NO CONTENT
     * @throws IOException to signify bad request
     */
    @DELETE
    @Path("{prefix}")
    public Response dhcpFpmDelete(@PathParam("prefix") String prefix) {
        DhcpRelayService dhcpRelayService = get(DhcpRelayService.class);
        RouteStore routeStore = get(RouteStore.class);

        try {
            // removes fpm route from fpm record
            Optional<FpmRecord> fpmRecord = dhcpRelayService.removeFpmRecord(IpPrefix.valueOf(prefix));
            if (fpmRecord.isPresent()) {
                IpAddress nextHop = fpmRecord.get().nextHop();
                Route route = new Route(Route.Source.DHCP, IpPrefix.valueOf(prefix), nextHop);
                // removes DHCP route from route store
                routeStore.removeRoute(route);
            } else {
                LOG.warn("fpmRecord is not present");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response.noContent().build();
    }
    /**
     * Returns the response object with list of dhcp servers without counters.
     *
     * @return 200 OK with component properties of given component and variable
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("dhcp-relay")
    public Response getDhcpServers() {
        ObjectNode node = getdhcpRelayJsonOutput(null, null);
        return Response.status(200).entity(node).build();
    }

    /**
     * Returns dhcp servers details with counters.
     *
     * @param counter source ip identifier
     * @return 200 OK with component properties of given component and variable
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("dhcp-relay/{counter}")
    public Response getDhcpRelayCounter(@PathParam("counter") String counter) {
        ObjectNode node = getdhcpRelayJsonOutput(counter, null);
        return Response.status(200).entity(node).build();
    }

    /**
     * To reset the dhcp relay counters.
     *
     * @param counter type String
     * @param reset   type String
     * @return 200 OK with component properties of given component and variable
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Path("dhcp-relay/{counter}/{reset}")
    public Response resetDhcpRelayCounter(@PathParam("counter") String counter, @PathParam("reset") String reset) {
        ObjectNode node = getdhcpRelayJsonOutput(counter, reset);
        return Response.status(200).entity(node).build();
    }


    /**
     * To create json output.
     *
     * @param counter type String
     * @param reset  type String
     * @return node type ObjectNode.
     */
    private ObjectNode getdhcpRelayJsonOutput(String counter, String reset) {
        ObjectNode node = mapper.createObjectNode();
        ObjectNode dhcpRelayServerNode = mapper.createObjectNode();
            if (!defaultDhcpServerInfoList.isEmpty()) {
                ArrayNode defaultDhcpServers = listServers(defaultDhcpServerInfoList);
                dhcpRelayServerNode.put("Default-DHCP-Server", defaultDhcpServers);
            }
            if (!indirectDhcpServerInfoList.isEmpty()) {
                ArrayNode indirectDhcpServers = listServers(indirectDhcpServerInfoList);
                dhcpRelayServerNode.put("Indirect-DHCP-Server", indirectDhcpServers);
            }

            ArrayNode dhcpRecords = dhcpRelayRecords(records);
            dhcpRelayServerNode.put("DHCP-Relay-Records([D]:Directly-Connected)", dhcpRecords);
            if (counter != null && !counter.equals("counter")) {
                ArrayNode counterArray = dhcpRelayCounters(reset);
                dhcpRelayServerNode.put("DHCP-Relay-Counter", counterArray);
            }
            node.put("Default-DHCP-Servers", dhcpRelayServerNode);

        return node;

    }
    /**
     * To get the liset of dhcp servers.
     *
     * @param dhcpServerInfoList type List
     * @return servers type ArrayNode.
     */
    private ArrayNode listServers(List<DhcpServerInfo> dhcpServerInfoList) {
        ArrayNode servers = mapper.createArrayNode();
        dhcpServerInfoList.forEach(dhcpServerInfo -> {
            ObjectNode serverNode = mapper.createObjectNode();
            String connectPoint = dhcpServerInfo.getDhcpServerConnectPoint()
                    .map(cp -> cp.toString()).orElse(NA);
            String serverMac = dhcpServerInfo.getDhcpConnectMac()
                    .map(mac -> mac.toString()).orElse(NA);
            String gatewayAddress;
            String serverIp;

            switch (dhcpServerInfo.getVersion()) {
                case DHCP_V4:
                    gatewayAddress = dhcpServerInfo.getDhcpGatewayIp4()
                            .map(gw -> gw.toString()).orElse(null);
                    serverIp = dhcpServerInfo.getDhcpServerIp4()
                            .map(ip -> ip.toString()).orElse(NA);
                    break;
                case DHCP_V6:
                    gatewayAddress = dhcpServerInfo.getDhcpGatewayIp6()
                            .map(gw -> gw.toString()).orElse(null);
                    serverIp = dhcpServerInfo.getDhcpServerIp6()
                            .map(ip -> ip.toString()).orElse(NA);
                    break;
                default:
                    return;
            }

            serverNode.put("connectPoint", connectPoint);
            if (gatewayAddress != null) {
                serverNode.put("server", serverIp.concat(" via ").concat(gatewayAddress));
            } else {
                serverNode.put("server", serverIp);
            }
            serverNode.put("mac", serverMac);
            servers.add(serverNode);
        });
       return servers;
}

    /**
     * To get the list of dhcp relay records.
     *
     * @param records type Collections
     * @return dhcpRelayRecords type ArrayNode.
     */
    private ArrayNode dhcpRelayRecords(Collection<DhcpRecord> records) {
        DhcpRelayCommand dhcpRelayCommand = new DhcpRelayCommand();
        ArrayNode dhcpRelayRecords = mapper.createArrayNode();
        records.forEach(record -> {
            ObjectNode dhcpRecord = mapper.createObjectNode();
            dhcpRecord.put("id", record.macAddress() + "/" + record.vlanId());
            dhcpRecord.put("locations", record.locations().toString());
            dhcpRecord.put("last-seen", Tools.timeAgo(record.lastSeen()));
            dhcpRecord.put("IPv4", dhcpRelayCommand.ip4State(record));
            dhcpRecord.put("IPv6", dhcpRelayCommand.ip6State(record));
            dhcpRelayRecords.add(dhcpRecord);
        });
        return dhcpRelayRecords;
    }

   /**
     * To get the details of dhcp relay counters.
     *
     * @param reset type String
     * @return counterArray type ArrayNode.
     */
    private ArrayNode dhcpRelayCounters(String reset) {
        ObjectNode counters = mapper.createObjectNode();
        ObjectNode counterPackets = mapper.createObjectNode();
        ArrayNode counterArray = mapper.createArrayNode();
        records.forEach(record -> {
            DhcpRelayCounters v6Counters = record.getV6Counters();
            if (reset != null && reset.equals("reset")) {
                v6Counters.resetCounters();
            }
            Map<String, Integer> countersMap = v6Counters.getCounters();
            countersMap.forEach((name, value) -> {
                counterPackets.put(name, value);

            });
            counters.put(record.locations().toString(), counterPackets);
            counterArray.add(counters);
        });
        return counterArray;
    }

}
