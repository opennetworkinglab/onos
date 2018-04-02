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

package org.onosproject.routeservice.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.routeservice.ResolvedRoute;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteAdminService;
import org.onosproject.routeservice.RouteService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Manage the unicast routing information.
 */
@Path("routes")
public class RouteServiceWebResource extends AbstractWebResource {

    protected static final String ROUTES = "routes";
    protected static final String ROUTES_KEY_ERROR = "Routes key must be present";

    /**
     * Get all unicast routes.
     * Returns array of all known unicast routes.
     *
     * @return 200 OK with array of all known unicast routes
     * @onos.rsModel RoutesGet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoutes() {
        RouteService service = get(RouteService.class);
        ObjectNode root = mapper().createObjectNode();
        service.getRouteTables().forEach(table -> {
            List<Route> routes = service.getRoutes(table).stream()
                    .flatMap(ri -> ri.allRoutes().stream())
                    .map(ResolvedRoute::route)
                    .collect(Collectors.toList());
            root.put(table.name(), codec(Route.class).encode(routes, this));
        });
        return ok(root).build();
    }

    /**
     * Create new unicast route.
     * Creates a new route in the unicast RIB. Routes created through the REST
     * API are always created as STATIC routes, so there is no need to specify
     * the type.
     *
     * @param route unicast route JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid, NO_CONTENT otherwise
     * @onos.rsModel RoutePost
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRoute(InputStream route) {
        RouteAdminService service = get(RouteAdminService.class);
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), route);
            Route r = codec(Route.class).decode(jsonTree, this);
            service.update(Collections.singletonList(r));
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response
                .noContent()
                .build();
    }

    /**
     * Creates new unicast routes.
     * Creates new routes in the unicast RIB. Routes created through the REST
     * API are always created as STATIC routes, so there is no need to specify
     * the type.
     *
     * @param routesStream unicast routes JSON array
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid, NO_CONTENT otherwise
     * @onos.rsModel RoutesPost
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/bulk")
    public Response createRoutes(InputStream routesStream) {
        RouteAdminService service = get(RouteAdminService.class);
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), routesStream);
            ArrayNode routesArray = nullIsIllegal((ArrayNode) jsonTree.get(ROUTES),
                    ROUTES_KEY_ERROR);
            List<Route> routes = codec(Route.class).decode(routesArray, this);
            service.update(routes);

        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response
                .noContent()
                .build();
    }

    /**
     * Remove a unicast route.
     * Removes a route from the unicast RIB.
     *
     * @param route unicast route JSON
     * @return 204 NO CONTENT
     * @onos.rsModel RoutePost
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRoute(InputStream route) {
        RouteAdminService service = get(RouteAdminService.class);
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), route);
            Route r = codec(Route.class).decode(jsonTree, this);
            service.withdraw(Collections.singletonList(r));
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return Response.noContent().build();
    }

    /**
     * Removes unicast routes.
     * Removes multiple routes from the unicast RIB.
     *
     * @param routesStream unicast routes array JSON
     * @return 204 NO CONTENT
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/bulk")
    public Response deleteRoutes(InputStream routesStream) {
        RouteAdminService service = get(RouteAdminService.class);
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), routesStream);
            ArrayNode routesArray = nullIsIllegal((ArrayNode) jsonTree.get(ROUTES),
                    ROUTES_KEY_ERROR);
            List<Route> routes = codec(Route.class).decode(routesArray, this);
            service.withdraw(routes);

        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response
                .noContent()
                .build();
    }
}
