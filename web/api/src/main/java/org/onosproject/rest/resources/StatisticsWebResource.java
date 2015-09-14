/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.net.device.DeviceService;
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
 * Query flow statistics.
 */
@Path("statistics")
public class StatisticsWebResource  extends AbstractWebResource {
    @Context
    UriInfo uriInfo;

    /**
     * Get load statistics for all links or for a specific link.
     *
     * @param deviceId (optional) device ID for a specific link
     * @param port (optional) port number for a specified link
     * @return JSON encoded array lof Load objects
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
     * Get table statistics for all tables of all devices.
     *
     * @return JSON encoded array of table statistics
     */
    @GET
    @Path("flows/tables")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTableStatistics() {
        final FlowRuleService service = get(FlowRuleService.class);
        final Iterable<Device> devices = get(DeviceService.class).getDevices();
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("device-table-statistics");
        for (final Device device : devices) {
            final ObjectNode deviceStatsNode = mapper().createObjectNode();
            deviceStatsNode.put("device", device.id().toString());
            final ArrayNode statisticsNode = deviceStatsNode.putArray("table-statistics");
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
     * Get table statistics for all tables of a specified device.
     *
     * @param deviceId device ID
     * @return JSON encoded array of table statistics
     */
    @GET
    @Path("flows/tables/{deviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTableStatisticsByDeviceId(@PathParam("deviceId") String deviceId) {
        final FlowRuleService service = get(FlowRuleService.class);
        final Iterable<TableStatisticsEntry> tableStatisticsEntries =
                service.getFlowTableStatistics(DeviceId.deviceId(deviceId));
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode rootArrayNode = root.putArray("table-statistics");

        final ObjectNode deviceStatsNode = mapper().createObjectNode();
        deviceStatsNode.put("device", deviceId);
        final ArrayNode statisticsNode = deviceStatsNode.putArray("table-statistics");
        for (final TableStatisticsEntry entry : tableStatisticsEntries) {
            statisticsNode.add(codec(TableStatisticsEntry.class).encode(entry, this));
        }
        rootArrayNode.add(deviceStatsNode);
        return ok(root).build();
    }
}
