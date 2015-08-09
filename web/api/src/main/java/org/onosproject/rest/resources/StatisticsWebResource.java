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
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.onosproject.codec.JsonCodec;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.rest.AbstractWebResource;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Statistics REST APIs.
 */
@Path("statistics")
public class StatisticsWebResource  extends AbstractWebResource {
    @Context
    UriInfo uriInfo;

    /**
     * Gets the Load statistics for all links, or for a specific link.
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
}
