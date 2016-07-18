/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

import org.onlab.util.HexString;
import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Query and program group rules.
 */

@Path("groups")
public class GroupsWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private static final String DEVICE_INVALID = "Invalid deviceId in group creation request";
    private static final String GROUP_NOT_FOUND = "Group was not found";

    private final GroupService groupService = get(GroupService.class);
    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode groupsNode = root.putArray("groups");

    /**
     * Returns all groups of all devices.
     *
     * @return 200 OK with array of all the groups in the system
     * @onos.rsModel Groups
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        final Iterable<Device> devices = get(DeviceService.class).getDevices();
        devices.forEach(device -> {
            final Iterable<Group> groups = groupService.getGroups(device.id());
            if (groups != null) {
                groups.forEach(group -> groupsNode.add(codec(Group.class).encode(group, this)));
            }
        });

        return ok(root).build();
    }

    /**
     * Returns all groups associated with the given device.
     *
     * @param deviceId device identifier
     * @return 200 OK with array of all the groups in the system
     * @onos.rsModel Groups
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}")
    public Response getGroupsByDeviceId(@PathParam("deviceId") String deviceId) {
        final Iterable<Group> groups = groupService.getGroups(DeviceId.deviceId(deviceId));

        groups.forEach(group -> groupsNode.add(codec(Group.class).encode(group, this)));

        return ok(root).build();
    }

    /**
     * Returns a group with the given deviceId and appCookie.
     *
     * @param deviceId device identifier
     * @param appCookie group key
     * @return 200 OK with a group entry in the system
     * @onos.rsModel Group
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{appCookie}")
    public Response getGroupByDeviceIdAndAppCookie(@PathParam("deviceId") String deviceId,
                                                   @PathParam("appCookie") String appCookie) {
        final DeviceId deviceIdInstance = DeviceId.deviceId(deviceId);

        if (!appCookie.startsWith("0x")) {
            throw new IllegalArgumentException("APP_COOKIE must be a hex string starts with 0x");
        }
        final GroupKey appCookieInstance = new DefaultGroupKey(HexString.fromHexString(
                appCookie.split("0x")[1], ""));

        Group group = nullIsNotFound(groupService.getGroup(deviceIdInstance, appCookieInstance),
                GROUP_NOT_FOUND);

        groupsNode.add(codec(Group.class).encode(group, this));
        return ok(root).build();
    }

    /**
     * Create new group rule. Creates and installs a new group rule for the
     * specified device.
     *
     * @param deviceId device identifier
     * @param stream   group rule JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel GroupsPost
     */
    @POST
    @Path("{deviceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroup(@PathParam("deviceId") String deviceId,
                                InputStream stream) {
        try {

            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedDeviceId = jsonTree.get("deviceId");

            if (specifiedDeviceId != null &&
                    !specifiedDeviceId.asText().equals(deviceId)) {
                throw new IllegalArgumentException(DEVICE_INVALID);
            }
            jsonTree.put("deviceId", deviceId);
            Group group = codec(Group.class).decode(jsonTree, this);
            GroupDescription description = new DefaultGroupDescription(
                    group.deviceId(), group.type(), group.buckets(),
                    group.appCookie(), group.id().id(), group.appId());
            groupService.addGroup(description);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("groups")
                    .path(deviceId)
                    .path(Long.toString(group.id().id()));
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Removes the specified group.
     *
     * @param deviceId  device identifier
     * @param appCookie application cookie to be used for lookup
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{deviceId}/{appCookie}")
    public Response deleteGroupByDeviceIdAndAppCookie(@PathParam("deviceId") String deviceId,
                                                      @PathParam("appCookie") String appCookie) {
        DeviceId deviceIdInstance = DeviceId.deviceId(deviceId);

        if (!appCookie.startsWith("0x")) {
            throw new IllegalArgumentException("APP_COOKIE must be a hex string starts with 0x");
        }
        GroupKey appCookieInstance = new DefaultGroupKey(HexString.fromHexString(
                appCookie.split("0x")[1], ""));

        groupService.removeGroup(deviceIdInstance, appCookieInstance, null);
        return Response.noContent().build();
    }
}
