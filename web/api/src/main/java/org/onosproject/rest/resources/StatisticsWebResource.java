/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.rest.resources;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TableStatisticsEntry;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.rest.AbstractWebResource;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Query statistics.
 */
@Path("statistics")
public class StatisticsWebResource  extends AbstractWebResource {
    @Context
    private UriInfo uriInfo;

    /**
     * Gets load statistics for all links or for a specific link.
     *
     * @onos.rsModel StatisticsFlowsLink
     * @param deviceId (optional) device ID for a specific link
     * @param port (optional) port number for a specified link
     * @return 200 OK with JSON encoded array of Load objects
     */
    @GET
    @Path("flows/link")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLoads(@QueryParam("device") String deviceId,
                             @QueryParam("port") String port) {
        Iterable<Link> links;

        if (deviceId == null || port == null) {
            links = get(LinkService.class).getLinks();
        } else {
            ConnectPoint connectPoint = new ConnectPoint(deviceId(deviceId),
                    portNumber(port));
            links = get(LinkService.class).getLinks(connectPoint);
        }
        ObjectNode result = mapper().createObjectNode();
        ArrayNode loads = mapper().createArrayNode();
        JsonCodec<Load> loadCodec = codec(Load.class);
        StatisticService statsService = getService(StatisticService.class);

        StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                links.iterator(), Spliterator.ORDERED), false)
                .forEach(link -> {
                    ObjectNode loadNode = loadCodec.encode(statsService.load(link), this);

                    UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                            .path("links")
                            .queryParam("device", link.src().deviceId().toString())
                            .queryParam("port", link.src().port().toString());
                    loadNode.put("link", locationBuilder.build().toString());
                    loads.add(loadNode);
                });
        result.set("loads", loads);
        return ok(result).build();
    }

    /**
     * Gets table statistics for all tables of all devices.
     *
     * @onos.rsModel StatisticsFlowsTables
     * @return 200 OK with JSON encoded array of table statistics
     */
    @GET
    @Path("flows/tables")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTableStatistics() {
        final FlowRuleService service = get(FlowRuleService.class);
        final Iterable<Device> devices = get(DeviceService.class).getDevices();
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");
        for (final Device device : devices) {
            final ObjectNode deviceStatsNode = mapper().createObjectNode();
            deviceStatsNode.put("device", device.id().toString());
            final ArrayNode statisticsNode = deviceStatsNode.putArray("table");
            final Iterable<TableStatisticsEntry> tableStatsEntries = service.getFlowTableStatistics(device.id());
            if (tableStatsEntries != null) {
                for (final TableStatisticsEntry entry : tableStatsEntries) {
                    statisticsNode.add(codec(TableStatisticsEntry.class).encode(entry, this));
                }
            }
            rootArrayNode.add(deviceStatsNode);
        }

        return ok(root).build();
    }

    /**
     * Gets table statistics for all tables of a specified device.
     *
     * @onos.rsModel StatisticsFlowsTables
     * @param deviceId device ID
     * @return 200 OK with JSON encoded array of table statistics
     */
    @GET
    @Path("flows/tables/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTableStatisticsByDeviceId(@PathParam("deviceId") String deviceId) {
        final FlowRuleService service = get(FlowRuleService.class);
        final Iterable<TableStatisticsEntry> tableStatisticsEntries =
                service.getFlowTableStatistics(DeviceId.deviceId(deviceId));
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");

        final ObjectNode deviceStatsNode = mapper().createObjectNode();
        deviceStatsNode.put("device", deviceId);
        final ArrayNode statisticsNode = deviceStatsNode.putArray("table");
        for (final TableStatisticsEntry entry : tableStatisticsEntries) {
            statisticsNode.add(codec(TableStatisticsEntry.class).encode(entry, this));
        }
        rootArrayNode.add(deviceStatsNode);
        return ok(root).build();
    }

    /**
     * Gets port statistics of all devices.
     * @onos.rsModel StatisticsPorts
     * @return 200 OK with JSON encoded array of port statistics
     */
    @GET
    @Path("ports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortStatistics() {
        final DeviceService service = get(DeviceService.class);
        final Iterable<Device> devices = service.getDevices();
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");
        for (final Device device : devices) {
            final ObjectNode deviceStatsNode = mapper().createObjectNode();
            deviceStatsNode.put("device", device.id().toString());
            final ArrayNode statisticsNode = deviceStatsNode.putArray("ports");
            final Iterable<PortStatistics> portStatsEntries = service.getPortStatistics(device.id());
            if (portStatsEntries != null) {
                for (final PortStatistics entry : portStatsEntries) {
                    statisticsNode.add(codec(PortStatistics.class).encode(entry, this));
                }
            }
            rootArrayNode.add(deviceStatsNode);
        }

        return ok(root).build();
    }

    /**
     * Gets port statistics of a specified devices.
     * @onos.rsModel StatisticsPorts
     * @param deviceId device ID
     * @return 200 OK with JSON encoded array of port statistics
     */
    @GET
    @Path("ports/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortStatisticsByDeviceId(@PathParam("deviceId") String deviceId) {
        final DeviceService service = get(DeviceService.class);
        final Iterable<PortStatistics> portStatsEntries =
                service.getPortStatistics(DeviceId.deviceId(deviceId));
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");
        final ObjectNode deviceStatsNode = mapper().createObjectNode();
        deviceStatsNode.put("device", deviceId);
        final ArrayNode statisticsNode = deviceStatsNode.putArray("ports");
        if (portStatsEntries != null) {
            for (final PortStatistics entry : portStatsEntries) {
                statisticsNode.add(codec(PortStatistics.class).encode(entry, this));
            }
        }
        rootArrayNode.add(deviceStatsNode);

        return ok(root).build();
    }

    /**
     * Gets port statistics of a specified device and port.
     * @onos.rsModel StatisticsPorts
     * @param deviceId device ID
     * @param port port
     * @return 200 OK with JSON encoded array of port statistics for the specified port
     */
    @GET
    @Path("ports/{deviceId}/{port}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortStatisticsByDeviceIdAndPort(@PathParam("deviceId") String deviceId,
                                                       @PathParam("port") String port) {
        final DeviceService service = get(DeviceService.class);
        final PortNumber portNumber = portNumber(port);
        final PortStatistics portStatsEntry =
                service.getStatisticsForPort(DeviceId.deviceId(deviceId), portNumber);
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");
        final ObjectNode deviceStatsNode = mapper().createObjectNode();
        deviceStatsNode.put("device", deviceId);
        final ArrayNode statisticsNode = deviceStatsNode.putArray("ports");
        if (portStatsEntry != null) {
            statisticsNode.add(codec(PortStatistics.class).encode(portStatsEntry, this));
        }
        rootArrayNode.add(deviceStatsNode);

        return ok(root).build();
    }

    /**
     * Gets port delta statistics of all devices.
     * @onos.rsModel StatisticsPorts
     * @return 200 OK with JSON encoded array of port delta statistics
     */
    @GET
    @Path("delta/ports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortDeltaStatistics() {
        final DeviceService service = get(DeviceService.class);
        final Iterable<Device> devices = service.getDevices();
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");
        for (final Device device : devices) {
            final ObjectNode deviceStatsNode = mapper().createObjectNode();
            deviceStatsNode.put("device", device.id().toString());
            final ArrayNode statisticsNode = deviceStatsNode.putArray("ports");
            final Iterable<PortStatistics> portStatsEntries = service.getPortDeltaStatistics(device.id());
            if (portStatsEntries != null) {
                for (final PortStatistics entry : portStatsEntries) {
                    statisticsNode.add(codec(PortStatistics.class).encode(entry, this));
                }
            }
            rootArrayNode.add(deviceStatsNode);
        }

        return ok(root).build();
    }

    /**
     * Gets port delta statistics of a specified devices.
     * @onos.rsModel StatisticsPorts
     * @param deviceId device ID
     * @return 200 OK with JSON encoded array of port delta statistics
     */
    @GET
    @Path("delta/ports/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortDeltaStatisticsByDeviceId(@PathParam("deviceId") String deviceId) {
        final DeviceService service = get(DeviceService.class);
        final Iterable<PortStatistics> portStatsEntries =
                service.getPortDeltaStatistics(DeviceId.deviceId(deviceId));
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");
        final ObjectNode deviceStatsNode = mapper().createObjectNode();
        deviceStatsNode.put("device", deviceId);
        final ArrayNode statisticsNode = deviceStatsNode.putArray("ports");
        if (portStatsEntries != null) {
            for (final PortStatistics entry : portStatsEntries) {
                statisticsNode.add(codec(PortStatistics.class).encode(entry, this));
            }
        }
        rootArrayNode.add(deviceStatsNode);

        return ok(root).build();
    }

    /**
     * Gets port delta statistics of a specified device and port.
     * @onos.rsModel StatisticsPorts
     * @param deviceId device ID
     * @param port port
     * @return 200 OK with JSON encoded array of port delta statistics for the specified port
     */
    @GET
    @Path("delta/ports/{deviceId}/{port}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPortDeltaStatisticsByDeviceIdAndPort(@PathParam("deviceId") String deviceId,
                                                       @PathParam("port") String port) {
        final DeviceService service = get(DeviceService.class);
        final PortNumber portNumber = portNumber(port);
        final PortStatistics portStatsEntry =
                service.getDeltaStatisticsForPort(DeviceId.deviceId(deviceId), portNumber);
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");
        final ObjectNode deviceStatsNode = mapper().createObjectNode();
        deviceStatsNode.put("device", deviceId);
        final ArrayNode statisticsNode = deviceStatsNode.putArray("ports");
        if (portStatsEntry != null) {
            statisticsNode.add(codec(PortStatistics.class).encode(portStatsEntry, this));
        }
        rootArrayNode.add(deviceStatsNode);

        return ok(root).build();
    }

    /**
     * Gets sum of active entries in all tables for all devices.
     *
     * @onos.rsModel StatisticsFlowsActiveEntries
     * @return 200 OK with JSON encoded array of active entry count per device
     */
    @GET
    @Path("flows/activeentries")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveEntriesCountPerDevice() {
        final FlowRuleService service = get(FlowRuleService.class);
        final Iterable<Device> devices = get(DeviceService.class).getDevices();
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("statistics");
        for (final Device device : devices) {
            int activeEntries = service.getFlowRuleCount(device.id(), FlowEntry.FlowEntryState.ADDED);
            final ObjectNode entry = mapper().createObjectNode();
            entry.put("device", device.id().toString());
            entry.put("activeEntries", activeEntries);
            rootArrayNode.add(entry);
        }

        return ok(root).build();
    }
}
