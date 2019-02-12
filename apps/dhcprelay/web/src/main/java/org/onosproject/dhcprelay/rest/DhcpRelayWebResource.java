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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.dhcprelay.api.DhcpRelayService;
import org.onosproject.dhcprelay.api.DhcpServerInfo;
import org.onosproject.dhcprelay.cli.DhcpRelayCommand;
import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.dhcprelay.store.DhcpRelayCounters;
import org.onosproject.dhcprelay.store.DhcpRelayCountersStore;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteStore;
import org.onosproject.routing.fpm.api.FpmRecord;
import org.slf4j.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * DHCP Relay agent REST API.
 */
@Path("dhcp-relay")
public class DhcpRelayWebResource extends AbstractWebResource {
    private static final Logger LOG = getLogger(DhcpRelayWebResource.class);
    private static final String NA = "N/A";
    private static final String HEADER_A_COUNTERS = "DHCP-Relay-Aggregate-Counters";
    private static final String GCOUNT_KEY = "global";
    private static final String DIRECTLY = "[D]";
    private static final String EMPTY = "";


    /**
     * Deletes the fpm route from fpm record.
     * Corresponding route from the route store
     *
     * @param prefix IpPrefix
     * @return 204 NO CONTENT, 404; 401
     */
    @DELETE
    @Path("fpm/{prefix}")
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
    public Response getDhcpRelayServers() {
        ObjectNode node = getDhcpRelayServersJsonOutput();
        return Response.status(200).entity(node).build();
    }

    /**
     * Returns dhcp counters details.
     *
     * @return 200 OK with component properties of given component and variable
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("counters")
    public Response getDhcpRelayCounters() {
        ObjectNode node = getDhcpRelayCountersJsonOutput();
        return Response.status(200).entity(node).build();
    }

    /**
     * To reset the dhcp relay counters.
     *
     * @return 200 OK with component properties of given component and variable
     */
    @DELETE
    @Path("counters")
    public Response resetDhcpRelayCounters() {
        resetDhcpRelayCountersInternal();
        return Response.status(200).build();
    }

    /**
     * Returns results with aggregate of counters.
     *
     * @return 200 OK with component properties of given component and variable
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("aggregate-counters")
    public Response getDhcpRelayAggCounters() {
        ObjectNode node = getDhcpRelayAggCountersJsonOutput();
        return Response.status(200).entity(node).build();
    }

    /**
     * Reset dhcp relay aggregate counters.
     *
     * @return 200 OK with component properties of given component and variable
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("aggregate-counters")
    public Response resetDhcpRelayAggCounters() {
        resetDhcpRelayAggCountersInternal();
        return Response.status(200).build();
    }

    /**
     * To create json output of Dhcp Relay servers.
     *
     * @return node type ObjectNode.
     */
    private ObjectNode getDhcpRelayServersJsonOutput() {
        ObjectNode node = mapper().createObjectNode();
        if (getDefaultDhcpServers().size() != 0) {
            node.put("Default-DHCP-Servers", getDefaultDhcpServers());
        }
        if (getIndirectDhcpServers().size() != 0) {
            node.put("Indirect-DHCP-Servers", getIndirectDhcpServers());
        }
        if (dhcpRelayRecords().size() != 0) {
            node.put("DHCP-Relay-Records", dhcpRelayRecords());
        }
        return node;
    }

    /**
     * To get the liset of dhcp servers.
     *
     * @param dhcpServerInfoList type List
     * @return servers type ArrayNode.
     */
    private ArrayNode listServers(List<DhcpServerInfo> dhcpServerInfoList) {
        ArrayNode servers = mapper().createArrayNode();
        dhcpServerInfoList.forEach(dhcpServerInfo -> {
            ObjectNode serverNode = mapper().createObjectNode();
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
     * @return dhcpRelayRecords type ArrayNode.
     */
    private ArrayNode dhcpRelayRecords() {
        DhcpRelayCommand dhcpRelayCommand = new DhcpRelayCommand();
        DhcpRelayService dhcpDelayService = get(DhcpRelayService.class);
        Collection<DhcpRecord> records = dhcpDelayService.getDhcpRecords();
        ObjectNode node = mapper().createObjectNode();
        ArrayNode dhcpRelayRecords = mapper().createArrayNode();
        records.forEach(record -> {
            ObjectNode dhcpRecord = mapper().createObjectNode();
            dhcpRecord.put("id", record.macAddress() + "/" + record.vlanId());
            dhcpRecord.put("locations", record.locations().toString()
                    .concat(record.directlyConnected() ? DIRECTLY : EMPTY));
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
     * @return counterArray type ArrayNode.
     */
    private ObjectNode getDhcpRelayCountersJsonOutput() {
        ObjectNode node = mapper().createObjectNode();
        ObjectNode counters = mapper().createObjectNode();
        DhcpRelayService dhcpDelayService = get(DhcpRelayService.class);
        Collection<DhcpRecord> records = dhcpDelayService.getDhcpRecords();
        ArrayNode counterArray = mapper().createArrayNode();
        records.forEach(record -> {
            DhcpRelayCounters v6Counters = record.getV6Counters();
            Map<String, Integer> countersMap = v6Counters.getCounters();
            ObjectNode counterPackets = mapper().createObjectNode();
            countersMap.forEach((name, value) -> {
                counterPackets.put(name, value);
            });
            counters.put(record.macAddress() + "/" + record.vlanId().toString(), counterPackets);
        });
        counterArray.add(counters);
        node.put("v6-DHCP-Relay-Counter", counterArray);
        return node;
    }

    /**
     * To get the list of default dhcp servers.
     *
     * @return node type ObjectNode.
     */
    private ArrayNode getDefaultDhcpServers() {
        ObjectNode node = mapper().createObjectNode();
        DhcpRelayService dhcpDelayService = get(DhcpRelayService.class);
        List<DhcpServerInfo> defaultDhcpServerInfoList = dhcpDelayService.getDefaultDhcpServerInfoList();
        ArrayNode defaultDhcpServers = listServers(defaultDhcpServerInfoList);
        return defaultDhcpServers;
    }

    /**
     * To get the list of indirect dhcp servers.
     *
     * @return node type ObjectNode.
     */
    private ArrayNode getIndirectDhcpServers() {
        DhcpRelayService dhcpDelayService = get(DhcpRelayService.class);
        List<DhcpServerInfo> indirectDhcpServerInfoList = dhcpDelayService.getIndirectDhcpServerInfoList();
        ArrayNode indirectDhcpServers = listServers(indirectDhcpServerInfoList);
        return indirectDhcpServers;
    }

    /**
     * To reset dhcp relay counters.
     *
     * @return counterArray type ArrayNode.
     */
    private void resetDhcpRelayCountersInternal() {
        DhcpRelayService dhcpDelayService = get(DhcpRelayService.class);
        Collection<DhcpRecord> records = dhcpDelayService.getDhcpRecords();
        records.forEach(record -> {
            DhcpRelayCounters v6Counters = record.getV6Counters();
            v6Counters.resetCounters();
        });
    }

    /**
     * To get dhcp relay aggregate counters.
     *
     * @return counterPackets type ObjectNode.
     */
    private ObjectNode getDhcpRelayAggCountersJsonOutput() {
        ObjectNode counterPackets = mapper().createObjectNode();
        ObjectNode dhcpRelayAggCounterNode = mapper().createObjectNode();
        DhcpRelayCountersStore counterStore = get(DhcpRelayCountersStore.class);
        Optional<DhcpRelayCounters> perClassCounters = counterStore.getCounters(GCOUNT_KEY);
        if (perClassCounters.isPresent()) {
            Map<String, Integer> counters = perClassCounters.get().getCounters();
            if (counters.size() > 0) {
                counters.forEach((name, value) -> {
                    counterPackets.put(name, value);
                });
            }
        }
        dhcpRelayAggCounterNode.put(HEADER_A_COUNTERS, counterPackets);
        return dhcpRelayAggCounterNode;
    }

    /**
     * To reset aggregate counters.
     *
     * @return counterPackets type ObjectNode.
     */
    private void resetDhcpRelayAggCountersInternal() {
        DhcpRelayCountersStore counterStore = get(DhcpRelayCountersStore.class);
        Optional<DhcpRelayCounters> perClassCounters = counterStore.getCounters(GCOUNT_KEY);
        if (perClassCounters.isPresent()) {
            counterStore.resetCounters(GCOUNT_KEY);
        }
    }

}
