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
package org.onosproject.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

/**
 * REST resource for interacting with the inventory of clusters.
 */

@Path("topology")
public class TopologyWebResource extends AbstractWebResource {

    public static final String CLUSTER_NOT_FOUND = "Cluster is not found";

    /**
     * Gets the topology overview for a REST GET operation.
     *
     * @return topology overview
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopology() {
        Topology topology = get(TopologyService.class).currentTopology();
        ObjectNode root =
                codec(Topology.class).encode(topology, this);
        return ok(root.toString()).build();
    }

    /**
     * Gets the topology clusters overview for a REST GET operation.
     *
     * @return topology clusters overview
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("clusters")
    public Response getClusters() {
        Topology topology = get(TopologyService.class).currentTopology();
        Iterable<TopologyCluster> clusters =
                get(TopologyService.class).getClusters(topology);
        ObjectNode root =
                encodeArray(TopologyCluster.class, "clusters", clusters);
        return ok(root.toString()).build();
    }

    /**
     * Gets details for a topology cluster for a REST GET operation.
     *
     * @return topology cluster details
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("clusters/{id}")
    public Response getCluster(@PathParam("id") int clusterId) {
        Topology topology = get(TopologyService.class).currentTopology();
        TopologyCluster cluster =
                nullIsNotFound(
                        get(TopologyService.class)
                                .getCluster(topology,
                                        ClusterId.clusterId(clusterId)),
                        CLUSTER_NOT_FOUND);
        ObjectNode root =
                codec(TopologyCluster.class).encode(cluster, this);
        return ok(root.toString()).build();
    }

    /**
     * Gets devices for a topology cluster for a REST GET operation.
     *
     * @return topology cluster devices
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("clusters/{id}/devices")
    public Response getClusterDevices(@PathParam("id") int clusterId) {
        Topology topology = get(TopologyService.class).currentTopology();
        TopologyCluster cluster =
                nullIsNotFound(
                        get(TopologyService.class)
                                .getCluster(topology,
                                        ClusterId.clusterId(clusterId)),
                        CLUSTER_NOT_FOUND);

        List<DeviceId> deviceIds =
                Lists.newArrayList(get(TopologyService.class)
                        .getClusterDevices(topology, cluster));

        ObjectNode root = mapper().createObjectNode();
        ArrayNode devicesNode = root.putArray("devices");

        for (DeviceId deviceId : deviceIds) {
            devicesNode.add(deviceId.toString());
        }
        return ok(root).build();
    }

    /**
     * Gets links for a topology cluster for a REST GET operation.
     *
     * @return topology cluster links
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("clusters/{id}/links")
    public Response getClusterLinks(@PathParam("id") int clusterId) {
        Topology topology = get(TopologyService.class).currentTopology();
        TopologyCluster cluster =
                nullIsNotFound(get(TopologyService.class).getCluster(topology,
                                ClusterId.clusterId(clusterId)),
                        CLUSTER_NOT_FOUND);

        List<Link> links =
                Lists.newArrayList(get(TopologyService.class)
                        .getClusterLinks(topology, cluster));

        return ok(encodeArray(Link.class, "links", links)).build();
    }

    /**
     * Extracts the port number portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return port number as a string, empty string if the port is not found
     */
    private static String getPortNumber(String deviceString) {
        int separator = deviceString.lastIndexOf(':');
        if (separator <= 0) {
            return "";
        }
        return deviceString.substring(separator + 1, deviceString.length());
    }

    /**
     * Extracts the device ID portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return device ID string
     */
    private static String getDeviceId(String deviceString) {
        int separator = deviceString.lastIndexOf(':');
        if (separator <= 0) {
            return "";
        }
        return deviceString.substring(0, separator);
    }

    /**
     * Gets the broadcast flag of a connect point for a REST GET operation.
     *
     * @return JSON representation of true if the connect point is broadcast,
     *         false otherwise
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("broadcast/{connectPoint}")
    public Response getConnectPointBroadcast(
            @PathParam("connectPoint") String connectPointString) {
        Topology topology = get(TopologyService.class).currentTopology();

        DeviceId deviceId = DeviceId.deviceId(getDeviceId(connectPointString));
        PortNumber portNumber = PortNumber.portNumber(getPortNumber(connectPointString));
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portNumber);
        boolean isBroadcast = get(TopologyService.class).isBroadcastPoint(topology, connectPoint);

        return ok(mapper().createObjectNode().put("broadcast", isBroadcast)).build();
    }

    /**
     * Gets the infrastructure flag of a connect point for a REST GET operation.
     *
     * @return JSON representation of true if the connect point is broadcast,
     *         false otherwise
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("infrastructure/{connectPoint}")
    public Response getConnectPointInfrastructure(
            @PathParam("connectPoint") String connectPointString) {
        Topology topology = get(TopologyService.class).currentTopology();

        DeviceId deviceId = DeviceId.deviceId(getDeviceId(connectPointString));
        PortNumber portNumber = PortNumber.portNumber(getPortNumber(connectPointString));
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portNumber);
        boolean isInfrastructure = get(TopologyService.class).isInfrastructure(topology, connectPoint);

        return ok(mapper().createObjectNode().put("infrastructure", isInfrastructure)).build();
    }

}
