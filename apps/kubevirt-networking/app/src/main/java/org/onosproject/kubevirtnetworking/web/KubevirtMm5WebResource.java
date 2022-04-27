/*
 * Copyright 2022-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.kubevirtnetworking.api.KubevirtFloatingIp;
import org.onosproject.kubevirtnetworking.api.KubevirtInstance;
import org.onosproject.kubevirtnetworking.api.KubevirtInstanceService;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancer;
import org.onosproject.kubevirtnetworking.api.KubevirtLoadBalancerService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtRouter;
import org.onosproject.kubevirtnetworking.api.KubevirtRouterService;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

/**
 * Handles REST API for communication with MEC Orchestrator.
 */
@Path("api/mm5/v1")
public class KubevirtMm5WebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String RECEIVED_REQUEST = "Received %s request";
    private static final String QUERY_STATUS_LEG = "LEG status query";
    private static final String QUERY_STATUS_VR = "VR status query";

    private static final String QUERY_GET_NETWORK = "get network query";
    private static final String QUERY_CREATE_NETWORK = "create network query";
    private static final String QUERY_UPDATE_NETWORK = "update network query";
    private static final String QUERY_DELETE_NETWORK = "delete network query";

    private static final String QUERY_GET_VR = "get virtual router query";
    private static final String QUERY_CREATE_VR = "create virtual router query";
    private static final String QUERY_UPDATE_VR = "update virtual router query";
    private static final String QUERY_DELETE_VR = "delete virtual router query";

    private static final String QUERY_GET_FIP = "get floating ip query";
    private static final String QUERY_CREATE_FIP = "create floating ip query";
    private static final String QUERY_UPDATE_FIP = "update floating ip query";
    private static final String QUERY_DELETE_FIP = "delete floating ip query";

    private static final String QUERY_GET_LB = "get LoadBalancer query";
    private static final String QUERY_CREATE_LB = "create LoadBalancer query";
    private static final String QUERY_UPDATE_LB = "update LoadBalancer query";
    private static final String QUERY_DELETE_LB = "delete LoadBalancer query";

    private static final String QUERY_GET_INSTANCE = "get instance query";
    private static final String QUERY_CREATE_INSTANCE = "create instance query";
    private static final String QUERY_UPDATE_INSTANCE = "update floating ip query";
    private static final String QUERY_DELETE_INSTANCE = "delete floating ip query";

    private static final String NODE_NAME = "nodeName";
    private static final String STATE = "state";
    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private static final String NETWORKS = "networks";
    private static final String INSTANCES = "instances";
    private static final String VIRTUAL_ROUTERS = "virtualRouters";
    private static final String LOAD_BALANCERS = "loadBalancers";
    private static final String FLOATING_IPS = "floatingIps";

    private static final String UP = "up";
    private static final String DOWN = "down";
    private static final String NONE = "none";

    private static final String VR_NAME = "vrName";

    @Context
    private UriInfo uriInfo;

    /**
     * Obtains the status of the virtual router.
     *
     * @param vrName virtual router name
     * @return the state of the virtual router in Json
     */
    @GET
    @Path("status/vr/{vrName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response vrStatus(@PathParam(VR_NAME) String vrName) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_STATUS_VR));

        KubevirtRouterService service = get(KubevirtRouterService.class);

        KubevirtRouter router = service.routers().stream()
                .filter(r -> r.name().equals(vrName))
                .findAny().orElse(null);

        if (router == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            ObjectNode jsonResult = mapper().createObjectNode();

            jsonResult.put(VR_NAME, router.name());

            if (router.electedGateway() == null) {
                jsonResult.put("merName", NONE);
                jsonResult.put("status", DOWN);
            } else {
                jsonResult.put("merName", router.electedGateway());
                jsonResult.put("status", UP);
            }

            jsonResult.put("timeUpdated",  System.currentTimeMillis());
            return ok(jsonResult).build();
        }
    }

    /**
     * Obtains the state of the leg node.
     *
     * @param nodeName leg host name
     * @return the state of the leg node in Json
     */
    @PUT
    @Path("state/mer/{nodeName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response legStatus(@PathParam("nodeName") String nodeName) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_STATUS_LEG));

        KubevirtNodeService service = get(KubevirtNodeService.class);

        ObjectNode jsonResult = mapper().createObjectNode();
        jsonResult.put(NODE_NAME, nodeName);

        boolean isActive = service.completeNodes().stream()
                .anyMatch(node -> node.type().equals(KubevirtNode.Type.GATEWAY) &&
                        node.hostname().equals(nodeName));
        if (isActive) {
            jsonResult.put(STATE, ACTIVE);
        } else {
            jsonResult.put(STATE, INACTIVE);
        }
        return ok(jsonResult).build();
    }

    /**
     * Obtains the network information in Json Array.
     *
     * @return network information in Json
     */
    @GET
    @Path("network")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetwork() {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_GET_NETWORK));

        KubevirtNetworkService service = get(KubevirtNetworkService.class);
        final Iterable<KubevirtNetwork> networks = service.networks();
        return ok(encodeArray(KubevirtNetwork.class, NETWORKS, networks)).build();
    }

    /**
     * Creates the kubevirt network with specified input stream.
     *
     * @param inputStream network Json input stream
     * @return 200 OK if succeeded
     */
    @POST
    @Path("network")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNetwork(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_CREATE_NETWORK));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Updates the kubevirt network with the specified input stream.
     *
     * @param inputStream network Json input stream
     * @return 200 OK if succeeded
     */
    @PUT
    @Path("network/{networkName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNetwork(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_UPDATE_NETWORK));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Deletes the kubevirt network with the specified input stream.
     *
     * @param inputStream network Json input stream
     * @return 200 OK if succeeded
     */
    @DELETE
    @Path("network/{networkName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNetwork(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_DELETE_NETWORK));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Obtains the instance information in Json Array.
     *
     * @return instance information in Json
     */
    @GET
    @Path("instance")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInstance() {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_GET_INSTANCE));

        KubevirtInstanceService service = get(KubevirtInstanceService.class);
        final Iterable<KubevirtInstance> instances = service.instances();
        return ok(encodeArray(KubevirtInstance.class, INSTANCES, instances)).build();
    }

    /**
     * Creates the instance with specified input stream.
     *
     * @param inputStream instance Json insput stream
     * @return 200 OK if succeeded
     */
    @POST
    @Path("instance")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createInstance(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_CREATE_INSTANCE));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Updates the instance with specified input stream.
     *
     * @param inputStream instance Json insput stream
     * @return 200 OK if succeeded
     */
    @PUT
    @Path("instance/{instanceName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateInstance(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_UPDATE_INSTANCE));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Deletes the instance with specified input stream.
     *
     * @param inputStream inputStream instance Json insput stream
     * @return 200 OK if succeeded
     */
    @DELETE
    @Path("instance/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteInstance(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_DELETE_INSTANCE));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Obtains the virtual router in Json array.
     *
     * @return virtual router information in Json
     */
    @GET
    @Path("vr")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVirtualRouter() {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_GET_VR));

        KubevirtRouterService service = get(KubevirtRouterService.class);
        final Iterable<KubevirtRouter> routers = service.routers();
        return ok(encodeArray(KubevirtRouter.class, VIRTUAL_ROUTERS, routers)).build();
    }

    /**
     * Creates the virtual router with specified input stream.
     *
     * @param inputStream virtual router Json inputstream
     * @return 200 OK if succeeded
     */
    @POST
    @Path("vr")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVirtualRouter(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_CREATE_VR));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Updates the virtual router with specified input stream.
     *
     * @param inputStream virtual router Json inputstream
     * @return 200 OK if succeeded
     */
    @PUT
    @Path("vr/{vrName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateVirtualRouter(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_UPDATE_VR));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Deletes the virtual router with specified input stream.
     *
     * @param inputStream virtual router Json inputstream
     * @return 200 OK if succeeded
     */
    @DELETE
    @Path("vr/{vrName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteVirtualRouter(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_DELETE_VR));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Obtains the floating ip in Json array.
     *
     * @return floating ip information in Json
     */
    @GET
    @Path("fip")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFloatingIp() {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_GET_FIP));

        KubevirtRouterService service = get(KubevirtRouterService.class);
        final Iterable<KubevirtFloatingIp> fips = service.floatingIps();
        return ok(encodeArray(KubevirtFloatingIp.class, FLOATING_IPS, fips)).build();
    }

    /**
     * Creates the floating ip with specified input stream.
     *
     * @param inputStream floating ip Json inputstream
     * @return 200 OK if succeeded
     */
    @POST
    @Path("fip")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFloatingIp(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_CREATE_FIP));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Updates the floating ip with specified input stream.
     *
     * @param inputStream floating ip Json inputstream
     * @return 200 OK if succeeded
     */
    @PUT
    @Path("fip/{fipName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFloatingIp(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_UPDATE_FIP));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Deletes the floating ip with specified input stream.
     *
     * @param inputStream floating ip Json inputstream
     * @return 200 OK if succeeded
     */
    @DELETE
    @Path("fip/{fipName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteFloatingIp(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_DELETE_FIP));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Obtains the loadbalaner in Json array.
     *
     * @return loadbalancer information in Json
     */
    @GET
    @Path("lb")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLoadBalancer() {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_GET_LB));

        KubevirtLoadBalancerService service = get(KubevirtLoadBalancerService.class);
        final Iterable<KubevirtLoadBalancer> lbs = service.loadBalancers();
        return ok(encodeArray(KubevirtLoadBalancer.class, LOAD_BALANCERS, lbs)).build();
    }

    /**
     * Creates the loadbalander with specified input stream.
     *
     * @param inputStream loadbalancer Json inputstream
     * @return 200 OK if succeeded
     */
    @POST
    @Path("lb")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLoadBalancer(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_CREATE_LB));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Updates the loadbalander with specified input stream.
     *
     * @param inputStream loadbalancer Json inputstream
     * @return 200 OK if succeeded
     */
    @PUT
    @Path("lb/{lbName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateLoadBalancer(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_UPDATE_LB));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Deletes the loadbalander with specified input stream.
     *
     * @param inputStream loadbalancer Json inputstream
     * @return 200 OK if succeeded
     */
    @DELETE
    @Path("lb/{lbName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteLoadBalancer(InputStream inputStream) {
        log.trace(String.format(RECEIVED_REQUEST, QUERY_DELETE_LB));
        //Just sends 200 OK for now.
        return ok(mapper().createObjectNode()).build();
    }
}
