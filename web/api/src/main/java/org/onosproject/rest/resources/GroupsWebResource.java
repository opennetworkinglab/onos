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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.onlab.util.HexString;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Query and program group rules.
 */

@Path("groups")
public class GroupsWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private static final String DEVICE_INVALID = "Invalid deviceId in group creation request";
    private static final String GROUP_NOT_FOUND = "Group was not found";
    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode groupsNode = root.putArray("groups");

    private GroupKey createKey(String appCookieString) {
        if (!appCookieString.startsWith("0x")) {
            throw new IllegalArgumentException("APP_COOKIE must be a hex string starts with 0x");
        }
        return  new DefaultGroupKey(HexString.fromHexString(
                appCookieString.split("0x")[1], ""));
    }

    /**
     * Returns all groups of all devices.
     *
     * @return 200 OK with array of all the groups in the system
     * @onos.rsModel Groups
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups() {
        GroupService groupService = get(GroupService.class);
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
        GroupService groupService = get(GroupService.class);
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
        GroupService groupService = get(GroupService.class);
        final DeviceId deviceIdInstance = DeviceId.deviceId(deviceId);

        final GroupKey appCookieInstance = createKey(appCookie);

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
        GroupService groupService = get(GroupService.class);
        try {

            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
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
        GroupService groupService = get(GroupService.class);
        DeviceId deviceIdInstance = DeviceId.deviceId(deviceId);

        final GroupKey appCookieInstance = createKey(appCookie);

        groupService.removeGroup(deviceIdInstance, appCookieInstance, null);
        return Response.noContent().build();
    }

    /**
     * Adds buckets to a group using the group service.
     *
     * @param deviceIdString device Id
     * @param appCookieString application cookie
     * @param stream JSON stream
     */
    private void updateGroupBuckets(String deviceIdString, String appCookieString, InputStream stream)
                 throws IOException {
        GroupService groupService = get(GroupService.class);
        DeviceId deviceId = DeviceId.deviceId(deviceIdString);
        final GroupKey groupKey = createKey(appCookieString);

        Group group = nullIsNotFound(groupService.getGroup(deviceId, groupKey), GROUP_NOT_FOUND);

        ObjectNode jsonTree = readTreeFromStream(mapper(), stream);

        GroupBuckets buckets = null;
        List<GroupBucket> groupBucketList = new ArrayList<>();
        JsonNode bucketsJson = jsonTree.get("buckets");
        final JsonCodec<GroupBucket> groupBucketCodec = codec(GroupBucket.class);
        if (bucketsJson != null) {
            IntStream.range(0, bucketsJson.size())
                    .forEach(i -> {
                        ObjectNode bucketJson = (ObjectNode) bucketsJson.get(i);
                        groupBucketList.add(groupBucketCodec.decode(bucketJson, this));
                    });
            buckets = new GroupBuckets(groupBucketList);
        }
        groupService.addBucketsToGroup(deviceId, groupKey, buckets, groupKey, group.appId());
    }

    /**
     * Adds buckets to an existing group.
     *
     * @param deviceIdString device identifier
     * @param appCookieString application cookie
     * @param stream  buckets JSON
     * @return status of the request - NO_CONTENT if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel GroupsBucketsPost
     */
    @POST
    @Path("{deviceId}/{appCookie}/buckets")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addBucket(@PathParam("deviceId") String deviceIdString,
                              @PathParam("appCookie") String appCookieString,
                              InputStream stream) {
        try {
            updateGroupBuckets(deviceIdString, appCookieString, stream);

            return Response
                    .noContent()
                    .build();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Removes buckets from a group using the group service.
     *
     * @param deviceIdString device Id
     * @param appCookieString application cookie
     * @param bucketIds comma separated list of bucket Ids to remove
     */
    private void removeGroupBuckets(String deviceIdString, String appCookieString, String bucketIds) {
        DeviceId deviceId = DeviceId.deviceId(deviceIdString);
        final GroupKey groupKey = createKey(appCookieString);
        GroupService groupService = get(GroupService.class);

        Group group = nullIsNotFound(groupService.getGroup(deviceId, groupKey), GROUP_NOT_FOUND);

        List<GroupBucket> groupBucketList = new ArrayList<>();

        List<String> bucketsToRemove = ImmutableList.copyOf(bucketIds.split(","));

        bucketsToRemove.forEach(
                bucketIdToRemove -> {
                    group.buckets().buckets().stream()
                            .filter(bucket -> Integer.toString(bucket.hashCode()).equals(bucketIdToRemove))
                            .forEach(groupBucketList::add);
                }
        );
        groupService.removeBucketsFromGroup(deviceId, groupKey,
                                            new GroupBuckets(groupBucketList), groupKey,
                                            group.appId());
    }

    /**
     * Removes buckets from an existing group.
     *
     * @param deviceIdString device identifier
     * @param appCookieString application cookie
     * @param bucketIds comma separated list of identifiers of buckets to remove from this group
     * @return status of the request - NO_CONTENT if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @DELETE
    @Path("{deviceId}/{appCookie}/buckets/{bucketIds}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBuckets(@PathParam("deviceId") String deviceIdString,
                                  @PathParam("appCookie") String appCookieString,
                                  @PathParam("bucketIds") String bucketIds) {
        removeGroupBuckets(deviceIdString, appCookieString, bucketIds);

        return Response
                .noContent()
                .build();
    }
}
