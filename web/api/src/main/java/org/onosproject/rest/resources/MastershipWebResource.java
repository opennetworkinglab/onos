/*
 * Copyright 2016-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Manage the mastership of ONOS instances.
 */
@Path("mastership")
public final class MastershipWebResource extends AbstractWebResource {

    private static final String DEVICE_IDS = "deviceIds";
    private static final String DEVICE_ID = "deviceId";
    private static final String NODE_ID = "nodeId";

    private static final String DEVICE_ID_INVALID = "Invalid deviceId for setting role";
    private static final String NODE_ID_INVALID = "Invalid nodeId for setting role";

    private static final String DEVICE_ID_NOT_FOUND = "Device Id is not found";
    private static final String NODE_ID_NOT_FOUND = "Node id is not found";
    private static final String ROLE_INFO_NOT_FOUND = "Role info is not found";
    private static final String MASTERSHIP_ROLE_NOT_FOUND = "Mastership role is not found";

    private final DeviceService deviceService = get(DeviceService.class);
    private final MastershipService mastershipService = get(MastershipService.class);
    private final MastershipAdminService mastershipAdminService =
                                         get(MastershipAdminService.class);

    /**
     * Returns the role of the local node for the specified device.
     *
     * @param deviceId device identifier
     * @return 200 OK with role of the current node
     * @onos.rsModel MastershipRole
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/local")
    public Response getLocalRole(@PathParam("deviceId") String deviceId) {
        MastershipRole role = mastershipService.getLocalRole(DeviceId.deviceId(deviceId));
        ObjectNode root = codec(MastershipRole.class).encode(role, this);
        return ok(root).build();
    }

    /**
     * Returns the current master for a given device.
     *
     * @param deviceId device identifier
     * @return 200 OK with the identifier of the master controller for the device
     * @onos.rsModel NodeId
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/master")
    public Response getMasterFor(@PathParam("deviceId") String deviceId) {
        NodeId id = nullIsNotFound(mastershipService.getMasterFor(
                    DeviceId.deviceId(deviceId)), NODE_ID_NOT_FOUND);

        ObjectNode root = mapper().createObjectNode();
        root.put(NODE_ID, id.id());
        return ok(root).build();
    }

    /**
     * Returns controllers connected to a given device, in order of
     * preference. The first entry in the list is the current master.
     *
     * @param deviceId device identifier
     * @return 200 OK with a list of controller identifiers
     * @onos.rsModel RoleInfo
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/role")
    public Response getNodesFor(@PathParam("deviceId") String deviceId) {
        RoleInfo info = nullIsNotFound(mastershipService.getNodesFor(
                        DeviceId.deviceId(deviceId)), ROLE_INFO_NOT_FOUND);
        ObjectNode root = codec(RoleInfo.class).encode(info, this);
        return ok(root).build();
    }

    /**
     * Returns the devices for which a controller is master.
     *
     * @param nodeId controller identifier
     * @return 200 OK with a set of device identifiers
     * @onos.rsModel DeviceIds
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{nodeId}/device")
    public Response getDeviceOf(@PathParam("nodeId") String nodeId) {
        ObjectNode root = mapper().createObjectNode();
        ArrayNode devicesNode = root.putArray(DEVICE_IDS);

        Set<DeviceId> devices = mastershipService.getDevicesOf(NodeId.nodeId(nodeId));
        if (devices != null) {
            devices.forEach(id -> devicesNode.add(id.toString()));
        }

        return ok(root).build();
    }

    /**
     * Returns the mastership status of the local controller for a given
     * device forcing master selection if necessary.
     *
     * @param deviceId device identifier
     * @return 200 OK with the role of this controller instance
     * @onos.rsModel MastershipRole
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/request")
    public Response requestRoleFor(@PathParam("deviceId") String deviceId) {
        DeviceId id = DeviceId.deviceId(deviceId);
        nullIsNotFound(deviceService.getDevice(id), DEVICE_ID_NOT_FOUND);

        MastershipRole role = nullIsNotFound(mastershipService.requestRoleForSync(id),
                        MASTERSHIP_ROLE_NOT_FOUND);
        ObjectNode root = codec(MastershipRole.class).encode(role, this);
        return ok(root).build();
    }

    /**
     * Abandons mastership of the specified device on the local node thus
     * forcing selection of a new master. If the local node is not a master
     * for this device, no master selection will occur.
     *
     * @param deviceId device identifier
     * @return status of the request - CREATED if the JSON is correct
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/relinquish")
    public Response relinquishMastership(@PathParam("deviceId") String deviceId) {
        DeviceId id = DeviceId.deviceId(deviceId);
        mastershipService.relinquishMastershipSync(id);
        return Response.created(id.uri()).build();
    }

    /**
     * Applies the current mastership role for the specified device.
     *
     * @param stream JSON representation of device, node, mastership info
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel MastershipPut
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setRole(InputStream stream) {

        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode deviceIdJson = jsonTree.get(DEVICE_ID);
            JsonNode nodeIdJson = jsonTree.get(NODE_ID);
            MastershipRole role = codec(MastershipRole.class).decode(jsonTree, this);

            if (deviceIdJson == null) {
                throw new IllegalArgumentException(DEVICE_ID_INVALID);
            }

            if (nodeIdJson == null) {
                throw new IllegalArgumentException(NODE_ID_INVALID);
            }

            mastershipAdminService.setRoleSync(NodeId.nodeId(nodeIdJson.asText()),
                    DeviceId.deviceId(deviceIdJson.asText()), role);

            return Response.ok().build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Balances the mastership to be shared as evenly as possibly by all
     * online instances.
     *
     * @return status of the request - OK if the request is successfully processed
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response balanceRoles() {
        mastershipAdminService.balanceRoles();
        return Response.ok().build();
    }
}
