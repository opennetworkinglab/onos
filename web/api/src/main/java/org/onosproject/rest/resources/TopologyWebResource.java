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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Query network topology graph and its components.
 */
@Path("topology")
public class TopologyWebResource extends AbstractWebResource {

    public static final String CLUSTER_NOT_FOUND = "Cluster is not found";

    /**
     * Get overview of current topology.
     *
     * @return topology overview
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTopology() {
        Topology topology = get(TopologyService.class).currentTopology();
        ObjectNode root = codec(Topology.class).encode(topology, this);
        return ok(root).build();
    }

    /**
     * Get overview of topology SCCs.
     *
     * @return topology clusters overview
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("clusters")
    public Response getClusters() {
        TopologyService service = get(TopologyService.class);
        Topology topology = service.currentTopology();
        Iterable<TopologyCluster> clusters = service.getClusters(topology);
        ObjectNode root = encodeArray(TopologyCluster.class, "clusters", clusters);
        return ok(root).build();
    }

    /**
     * Get details of a specific SCC.
     *
     * @param clusterId id of the cluster to query
     * @return topology cluster details
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("clusters/{id}")
    public Response getCluster(@PathParam("id") int clusterId) {
        Topology topology = get(TopologyService.class).currentTopology();
        TopologyCluster cluster = getTopologyCluster(clusterId, topology);
        ObjectNode root = codec(TopologyCluster.class).encode(cluster, this);
        return ok(root).build();
    }

    private TopologyCluster getTopologyCluster(int clusterId, Topology topology) {
        return nullIsNotFound(
                get(TopologyService.class)
                        .getCluster(topology, ClusterId.clusterId(clusterId)),
                CLUSTER_NOT_FOUND);
    }

    /**
     * Get devices in a specific SCC.
     *
     * @param clusterId id of the cluster to query
     * @return topology cluster devices
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("clusters/{id}/devices")
    public Response getClusterDevices(@PathParam("id") int clusterId) {
        TopologyService service = get(TopologyService.class);
        Topology topology = service.currentTopology();
        TopologyCluster cluster = getTopologyCluster(clusterId, topology);

        List<DeviceId> deviceIds =
                Lists.newArrayList(service.getClusterDevices(topology, cluster));

        ObjectNode root = mapper().createObjectNode();
        ArrayNode devicesNode = root.putArray("devices");
        deviceIds.forEach(id -> devicesNode.add(id.toString()));
        return ok(root).build();
    }

    /**
     * Get links in specific SCC.
     *
     * @param clusterId id of the cluster to query
     * @return topology cluster links
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("clusters/{id}/links")
    public Response getClusterLinks(@PathParam("id") int clusterId) {
        Topology topology = get(TopologyService.class).currentTopology();
        TopologyCluster cluster = getTopologyCluster(clusterId, topology);

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
     * Test if a connect point is in broadcast set.
     *
     * @param connectPointString deviceid:portnumber
     * @return JSON representation of true if the connect point is broadcast,
     *         false otherwise
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("broadcast/{connectPoint}")
    public Response getConnectPointBroadcast(@PathParam("connectPoint") String connectPointString) {
        Topology topology = get(TopologyService.class).currentTopology();

        DeviceId deviceId = DeviceId.deviceId(getDeviceId(connectPointString));
        PortNumber portNumber = PortNumber.portNumber(getPortNumber(connectPointString));
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portNumber);
        boolean isBroadcast = get(TopologyService.class).isBroadcastPoint(topology, connectPoint);

        return ok(mapper()
                .createObjectNode()
                .put("broadcast", isBroadcast))
                .build();
    }

    /**
     * Test if a connect point is infrastructure or edge.
     *
     * @param connectPointString deviceid:portnumber
     * @return JSON representation of true if the connect point is broadcast,
     *         false otherwise
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("infrastructure/{connectPoint}")
    public Response getConnectPointInfrastructure(@PathParam("connectPoint") String connectPointString) {
        Topology topology = get(TopologyService.class).currentTopology();

        DeviceId deviceId = DeviceId.deviceId(getDeviceId(connectPointString));
        PortNumber portNumber = PortNumber.portNumber(getPortNumber(connectPointString));
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portNumber);
        boolean isInfrastructure = get(TopologyService.class).isInfrastructure(topology, connectPoint);

        return ok(mapper()
                .createObjectNode()
                .put("infrastructure", isInfrastructure))
                .build();
    }

}
