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

import org.onosproject.net.Direction;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.link.LinkService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;


/**
 * Manage inventory of infrastructure links.
 */
@Path("links")
public class LinksWebResource extends AbstractWebResource {

    /**
     * Gets infrastructure links.
     * Returns array of all links, or links for the specified device or port.
     * @onos.rsModel LinksGet
     * @param deviceId  (optional) device identifier
     * @param port      (optional) port number
     * @param direction (optional) direction qualifier
     * @return 200 OK with array of all links, or links for the specified device or port
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLinks(@QueryParam("device") String deviceId,
                             @QueryParam("port") String port,
                             @QueryParam("direction") String direction) {
        LinkService service = get(LinkService.class);
        Iterable<Link> links;

        if (deviceId != null && port != null) {
            links = getConnectPointLinks(new ConnectPoint(deviceId(deviceId),
                                                          portNumber(port)),
                                         direction, service);
        } else if (deviceId != null) {
            links = getDeviceLinks(deviceId(deviceId), direction, service);
        } else {
            links = service.getLinks();
        }
        return ok(encodeArray(Link.class, "links", links)).build();
    }

    private Iterable<Link> getConnectPointLinks(ConnectPoint point,
                                                String direction,
                                                LinkService service) {
        Direction dir = direction != null ?
                Direction.valueOf(direction.toUpperCase()) : Direction.ALL;
        switch (dir) {
            case INGRESS:
                return service.getIngressLinks(point);
            case EGRESS:
                return service.getEgressLinks(point);
            default:
                return service.getLinks(point);
        }
    }

    private Iterable<Link> getDeviceLinks(DeviceId deviceId,
                                          String direction,
                                          LinkService service) {
        Direction dir = direction != null ?
                Direction.valueOf(direction.toUpperCase()) : Direction.ALL;
        switch (dir) {
            case INGRESS:
                return service.getDeviceIngressLinks(deviceId);
            case EGRESS:
                return service.getDeviceEgressLinks(deviceId);
            default:
                return service.getDeviceLinks(deviceId);
        }
    }

    /**
     * Get useStaleLinkAge active status.
     * Returns current status of the VanishedStaleLink.
     *
     * @onos.rsModel VanishedLink
     * @return 200 ok with the VanishedStaleLink status.
     */
    @GET
    @Path("{usestalelinkage}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVanishStaleLink() {
        ObjectNode root = mapper().createObjectNode();
        ComponentConfigService useStaleLink = get(ComponentConfigService.class);

        for (ConfigProperty prop : useStaleLink.getProperties("org.onosproject.provider.lldp.impl.LldpLinkProvider")) {
            if (prop.name().equals("useStaleLinkAge")) {
                root.put("active", Boolean.valueOf(prop.value()));
                break;
            }
        }
        return ok(root).build();
    }

    /**
     * Set useStaleLinkAge status.
     *
     * @onos.rsModel VanishedLink
     * @param stream input JSON
     * @return 200 ok.
     * BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Path("{usestalelinkage}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setVanishStaleLink(InputStream stream) {
        try {
            // Parse the input stream
            ObjectNode root = (ObjectNode) mapper().readTree(stream);
            if (root.has("active")) {
                ComponentConfigService useStaleLink = get(ComponentConfigService.class);
                useStaleLink.setProperty("org.onosproject.provider.lldp.impl.LldpLinkProvider",
                   "useStaleLinkAge", String.valueOf(root.get("active")));
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return Response
                .ok()
                .build();
    }

}
