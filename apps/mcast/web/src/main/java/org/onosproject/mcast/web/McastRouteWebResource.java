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

package org.onosproject.mcast.web;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import org.onlab.packet.IpAddress;
import org.onosproject.mcast.api.McastRoute;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Manage the multicast routing information.
 */
@Beta
@Path("mcast")
public class McastRouteWebResource extends AbstractWebResource {

    //TODO return error messages

    private static final String SOURCES = "sources";
    private static final String SINKS = "sinks";
    private static final String ROUTES = "routes";
    private static final String ROUTES_KEY_ERROR = "No routes";
    private static final String ASM = "*";

    private Optional<McastRoute> getStaticRoute(Set<McastRoute> mcastRoutes) {
        return mcastRoutes.stream()
                .filter(mcastRoute -> mcastRoute.type() == McastRoute.Type.STATIC)
                .findAny();
    }

    /**
     * Get all multicast routes.
     * Returns array of all known multicast routes.
     *
     * @return 200 OK with array of all known multicast routes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoutes() {
        Set<McastRoute> routes = get(MulticastRouteService.class).getRoutes();
        ObjectNode root = encodeArray(McastRoute.class, ROUTES, routes);
        return ok(root).build();
    }

    /**
     * Gets a multicast route.
     *
     * @param group group IP address
     * @param srcIp source IP address
     * @return 200 OK with a multicast routes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{group}/{srcIp}")
    public Response getRoute(@PathParam("group") String group,
                             @PathParam("srcIp") String srcIp) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ObjectNode root = encode(route.get(), McastRoute.class);
            return ok(root).build();
        }
        return Response.noContent().build();
    }

    /**
     * Get all sources connect points for a multicast route.
     *
     * @param group group IP address
     * @param srcIp source IP address
     * @return 200 OK with array of all sources for multicast route
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sources/{group}/{srcIp}")
    public Response getSources(@PathParam("group") String group,
                               @PathParam("srcIp") String srcIp) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ObjectNode sources = this.mapper().createObjectNode();
            get(MulticastRouteService.class).routeData(route.get()).sources().forEach((k, v) -> {
                ArrayNode node = this.mapper().createArrayNode();
                v.forEach(source -> {
                    node.add(source.toString());
                });
                sources.putPOJO(k.toString(), node);
            });
            ObjectNode root = this.mapper().createObjectNode().putPOJO(SOURCES, sources);
            return ok(root).build();
        }
        return Response.noContent().build();
    }

    /**
     * Get all HostId sinks and their connect points for a multicast route.
     *
     * @param group group IP address
     * @param srcIp source IP address
     * @return 200 OK with array of all sinks for multicast route
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sinks/{group}/{srcIp}")
    public Response getSinks(@PathParam("group") String group,
                             @PathParam("srcIp") String srcIp) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ObjectNode sinks = this.mapper().createObjectNode();
            get(MulticastRouteService.class).routeData(route.get()).sinks().forEach((k, v) -> {
                ArrayNode node = this.mapper().createArrayNode();
                v.forEach(sink -> {
                    node.add(sink.toString());
                });
                sinks.putPOJO(k.toString(), node);
            });
            ObjectNode root = this.mapper().createObjectNode().putPOJO(SINKS, sinks);
            return ok(root).build();
        }
        return Response.noContent().build();
    }

    /**
     * Get all source connect points for a given sink host in a multicast route.
     *
     * @param group  group IP address
     * @param srcIp  source IP address
     * @param hostId host Id
     * @return 200 OK with array of all sources for multicast route
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sources/{group}/{srcIp}/{hostId}")
    public Response getHostSources(@PathParam("group") String group,
                                   @PathParam("srcIp") String srcIp,
                                   @PathParam("hostId") String hostId) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ArrayNode node = this.mapper().createArrayNode();
            get(MulticastRouteService.class).sources(route.get(), HostId.hostId(hostId))
                    .forEach(source -> {
                        node.add(source.toString());
                    });
            ObjectNode root = this.mapper().createObjectNode().putPOJO(SOURCES, node);
            return ok(root).build();
        }
        return Response.noContent().build();
    }

    /**
     * Get all sink connect points for a given sink host in a multicast route.
     *
     * @param group  group IP address
     * @param srcIp  source IP address
     * @param hostId host Id
     * @return 200 OK with array of all sinks for multicast route
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sinks/{group}/{srcIp}/{hostId}")
    public Response getHostSinks(@PathParam("group") String group,
                                 @PathParam("srcIp") String srcIp,
                                 @PathParam("hostId") String hostId) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ArrayNode node = this.mapper().createArrayNode();
            get(MulticastRouteService.class).sinks(route.get(), HostId.hostId(hostId))
                    .forEach(source -> {
                        node.add(source.toString());
                    });
            ObjectNode root = this.mapper().createObjectNode().putPOJO(SINKS, node);
            return ok(root).build();
        }
        return Response.noContent().build();
    }

    /**
     * Creates a set of new multicast routes.
     *
     * @param stream multicast routes JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel McastRouteBulk
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("bulk/")
    public Response createRoutes(InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            ArrayNode routesArray = nullIsIllegal((ArrayNode) jsonTree.get(ROUTES),
                    ROUTES_KEY_ERROR);
            routesArray.forEach(routeJson -> {
                McastRoute route = codec(McastRoute.class).decode((ObjectNode) routeJson, this);
                service.add(route);

                Set<HostId> sources = new HashSet<>();
                routeJson.path(SOURCES).elements().forEachRemaining(src -> {
                    sources.add(HostId.hostId(src.asText()));
                });
                Set<HostId> sinks = new HashSet<>();
                routeJson.path(SINKS).elements().forEachRemaining(sink -> {
                    sinks.add(HostId.hostId(sink.asText()));
                });

                if (!sources.isEmpty()) {
                    sources.forEach(source -> {
                        service.addSource(route, source);
                    });
                }
                if (!sinks.isEmpty()) {
                    sinks.forEach(sink -> {
                        service.addSink(route, sink);
                    });
                }
            });
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response
                .created(URI.create(""))
                .build();
    }

    /**
     * Create new multicast route.
     *
     * @param stream multicast route JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel McastRoute
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoute(InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            McastRoute route = codec(McastRoute.class).decode(jsonTree, this);
            service.add(route);

            Set<HostId> sources = new HashSet<>();
            jsonTree.path(SOURCES).elements().forEachRemaining(src -> {
                sources.add(HostId.hostId(src.asText()));
            });
            Set<HostId> sinks = new HashSet<>();
            jsonTree.path(SINKS).elements().forEachRemaining(sink -> {
                sinks.add(HostId.hostId(sink.asText()));
            });

            if (!sources.isEmpty()) {
                sources.forEach(source -> {
                    service.addSource(route, source);
                });
            }
            if (!sinks.isEmpty()) {
                sinks.forEach(sink -> {
                    service.addSink(route, sink);
                });
            }

        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response
                .created(URI.create(""))
                .build();
    }

    /**
     * Adds sources for a given existing multicast route.
     *
     * @param group  group IP address
     * @param srcIp  source IP address
     * @param stream host sinks JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel McastSourcesAdd
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sources/{group}/{srcIp}")
    public Response addSources(@PathParam("group") String group,
                               @PathParam("srcIp") String srcIp,
                               InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ArrayNode jsonTree;
            try {
                jsonTree = (ArrayNode) mapper().readTree(stream).get(SOURCES);
                Set<HostId> sources = new HashSet<>();
                jsonTree.elements().forEachRemaining(src -> {
                    sources.add(HostId.hostId(src.asText()));
                });
                if (!sources.isEmpty()) {
                    sources.forEach(src -> {
                        service.addSource(route.get(), src);
                    });
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            return Response.ok().build();
        }
        return Response.noContent().build();

    }

    /**
     * Adds sinks for a given existing multicast route.
     *
     * @param group  group IP address
     * @param srcIp  source IP address
     * @param stream host sinks JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel McastSinksAdd
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sinks/{group}/{srcIp}")
    public Response addSinks(@PathParam("group") String group,
                             @PathParam("srcIp") String srcIp,
                             InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ArrayNode jsonTree;
            try {
                jsonTree = (ArrayNode) mapper().readTree(stream).get(SINKS);
                Set<HostId> sinks = new HashSet<>();
                jsonTree.elements().forEachRemaining(sink -> {
                    sinks.add(HostId.hostId(sink.asText()));
                });
                if (!sinks.isEmpty()) {
                    sinks.forEach(sink -> {
                        service.addSink(route.get(), sink);
                    });
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    /**
     * Adds a new set of connect points for an existing host source in a given multicast route.
     *
     * @param group  group IP address
     * @param srcIp  source IP address
     * @param hostId the host Id
     * @param stream source connect points JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel McastHostSourcesAdd
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("source/{group}/{srcIp}/{hostId}")
    public Response addHostSource(@PathParam("group") String group,
                                  @PathParam("srcIp") String srcIp,
                                  @PathParam("hostId") String hostId,
                                  InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ArrayNode jsonTree;
            try {
                jsonTree = (ArrayNode) mapper().readTree(stream).get(SOURCES);
                Set<ConnectPoint> sources = new HashSet<>();
                jsonTree.elements().forEachRemaining(src -> {
                    sources.add(ConnectPoint.deviceConnectPoint(src.asText()));
                });
                if (!sources.isEmpty()) {
                    service.addSources(route.get(), HostId.hostId(hostId), sources);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    /**
     * Adds a new sink for an existing host in a given multicast route.
     *
     * @param group  group IP address
     * @param srcIp  source IP address
     * @param hostId the host Id
     * @param stream sink connect points JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel McastHostSinksAdd
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sinks/{group}/{srcIp}/{hostId}")
    public Response addHostSinks(@PathParam("group") String group,
                                 @PathParam("srcIp") String srcIp,
                                 @PathParam("hostId") String hostId,
                                 InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        if (route.isPresent()) {
            ArrayNode jsonTree;
            try {
                jsonTree = (ArrayNode) mapper().readTree(stream).get(SINKS);
                Set<ConnectPoint> sinks = new HashSet<>();
                jsonTree.elements().forEachRemaining(src -> {
                    sinks.add(ConnectPoint.deviceConnectPoint(src.asText()));
                });
                if (!sinks.isEmpty()) {
                    service.addSinks(route.get(), HostId.hostId(hostId), sinks);
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
            return Response.ok().build();
        }
        return Response.noContent().build();
    }

    /**
     * Removes all the multicast routes.
     *
     * @return 204 NO CONTENT
     */
    @DELETE
    public Response deleteRoutes() {
        MulticastRouteService service = get(MulticastRouteService.class);
        service.getRoutes().forEach(service::remove);
        return Response.noContent().build();
    }

    /**
     * Removes all the given multicast routes.
     *
     * @param stream the set of multicast routes
     * @return 204 NO CONTENT
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("bulk/")
    public Response deleteRoutes(InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            ArrayNode routesArray = nullIsIllegal((ArrayNode) jsonTree.get(ROUTES),
                    ROUTES_KEY_ERROR);
            List<McastRoute> routes = codec(McastRoute.class).decode(routesArray, this);
            routes.forEach(service::remove);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return Response.noContent().build();
    }

    /**
     * Deletes a specific route.
     *
     * @param group group IP address
     * @param srcIp source IP address
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{group}/{srcIp}")
    public Response deleteRoute(@PathParam("group") String group,
                                @PathParam("srcIp") String srcIp) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        route.ifPresent(mcastRoute -> {
            get(MulticastRouteService.class).remove(mcastRoute);
        });
        return Response.noContent().build();
    }

    /**
     * Deletes all the source connect points for a specific route.
     * If the sources are empty the entire route is removed.
     *
     * @param group group IP address
     * @param srcIp source IP address
     * @return 204 NO CONTENT
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("sources/{group}/{srcIp}")
    public Response deleteSources(@PathParam("group") String group,
                                  @PathParam("srcIp") String srcIp) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        route.ifPresent(mcastRoute -> get(MulticastRouteService.class).removeSources(mcastRoute));
        return Response.noContent().build();
    }

    /**
     * Deletes a source hostId for a specific route.
     * If the sources are empty the entire route is removed.
     *
     * @param group  group IP address
     * @param srcIp  source IP address
     * @param hostId source host id
     * @return 204 NO CONTENT
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("sources/{group}/{srcIp}/{hostId}")
    public Response deleteSource(@PathParam("group") String group,
                                 @PathParam("srcIp") String srcIp,
                                 @PathParam("hostId") String hostId) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        route.ifPresent(mcastRoute -> get(MulticastRouteService.class)
                .removeSource(mcastRoute, HostId.hostId(hostId)));
        return Response.noContent().build();
    }

    /**
     * Deletes all the sinks for a specific route.
     *
     * @param group group IP address
     * @param srcIp source IP address
     * @return 204 NO CONTENT
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("sinks/{group}/{srcIp}")
    public Response deleteHostsSinks(@PathParam("group") String group,
                                     @PathParam("srcIp") String srcIp) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        route.ifPresent(mcastRoute -> get(MulticastRouteService.class)
                .removeSinks(mcastRoute));
        return Response.noContent().build();
    }

    /**
     * Deletes a sink connect points for a given host for a specific route.
     *
     * @param group  group IP address
     * @param srcIp  source IP address
     * @param hostId sink host
     * @return 204 NO CONTENT
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("sinks/{group}/{srcIp}/{hostId}")
    public Response deleteHostSinks(@PathParam("group") String group,
                                    @PathParam("srcIp") String srcIp,
                                    @PathParam("hostId") String hostId) {
        Optional<McastRoute> route = getMcastRoute(group, srcIp);
        route.ifPresent(mcastRoute -> get(MulticastRouteService.class)
                .removeSink(mcastRoute, HostId.hostId(hostId)));
        return Response.noContent().build();
    }

    private Optional<McastRoute> getMcastRoute(String group, String srcIp) {
        IpAddress ipAddress = null;
        if (!srcIp.equals(ASM)) {
            ipAddress = IpAddress.valueOf(srcIp);
        }
        return getStaticRoute(get(MulticastRouteService.class)
                .getRoute(IpAddress.valueOf(group), ipAddress));
    }

}
