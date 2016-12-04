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
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionAdminService;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.region.RegionService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Manages region and device membership.
 */
@Path("regions")
public class RegionsWebResource extends AbstractWebResource {
    private final RegionService regionService = get(RegionService.class);
    private final RegionAdminService regionAdminService = get(RegionAdminService.class);

    private static final String REGION_NOT_FOUND = "Region is not found for ";
    private static final String REGION_INVALID = "Invalid regionId in region update request";
    private static final String DEVICE_IDS_INVALID = "Invalid device identifiers";

    /**
     * Returns set of all regions.
     *
     * @return 200 OK with set of all regions
     * @onos.rsModel Regions
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRegions() {
        final Iterable<Region> regions = regionService.getRegions();
        return ok(encodeArray(Region.class, "regions", regions)).build();
    }

    /**
     * Returns the region with the specified identifier.
     *
     * @param regionId region identifier
     * @return 200 OK with a region, 404 not found
     * @onos.rsModel Region
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{regionId}")
    public Response getRegionById(@PathParam("regionId") String regionId) {
        final RegionId rid = RegionId.regionId(regionId);
        final Region region = nullIsNotFound(regionService.getRegion(rid),
                REGION_NOT_FOUND + rid.toString());
        return ok(codec(Region.class).encode(region, this)).build();
    }

    /**
     * Returns the set of devices that belong to the specified region.
     *
     * @param regionId region identifier
     * @return 200 OK with set of devices that belong to the specified region
     * @onos.rsModel RegionDeviceIds
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{regionId}/devices")
    public Response getRegionDevices(@PathParam("regionId") String regionId) {
        final RegionId rid = RegionId.regionId(regionId);
        final Iterable<DeviceId> deviceIds = regionService.getRegionDevices(rid);
        final ObjectNode root = mapper().createObjectNode();
        final ArrayNode deviceIdsNode = root.putArray("deviceIds");
        deviceIds.forEach(did -> deviceIdsNode.add(did.toString()));
        return ok(root).build();
    }

    /**
     * Creates a new region using the supplied JSON input stream.
     *
     * @param stream region JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel RegionPost
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRegion(InputStream stream) {
        URI location;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            final Region region = codec(Region.class).decode(jsonTree, this);
            final Region resultRegion = regionAdminService.createRegion(region.id(),
                                region.name(), region.type(), region.masters());
            location = new URI(resultRegion.id().id());
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.created(location).build();
    }

    /**
     * Updates the specified region using the supplied JSON input stream.
     *
     * @param regionId region identifier
     * @param stream region JSON stream
     * @return status of the request - UPDATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel RegionPost
     */
    @PUT
    @Path("{regionId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRegion(@PathParam("regionId") String regionId,
                                 InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedRegionId = jsonTree.get("id");

            if (specifiedRegionId != null &&
                    !specifiedRegionId.asText().equals(regionId)) {
                throw new IllegalArgumentException(REGION_INVALID);
            }

            final Region region = codec(Region.class).decode(jsonTree, this);
            regionAdminService.updateRegion(region.id(),
                                region.name(), region.type(), region.masters());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.ok().build();
    }

    /**
     * Removes the specified region using the given region identifier.
     *
     * @param regionId region identifier
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{regionId}")
    public Response removeRegion(@PathParam("regionId") String regionId) {
        final RegionId rid = RegionId.regionId(regionId);
        regionAdminService.removeRegion(rid);
        return Response.noContent().build();
    }

    /**
     * Adds the specified collection of devices to the region.
     *
     * @param regionId region identifier
     * @param stream deviceIds JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel RegionDeviceIds
     */
    @POST
    @Path("{regionId}/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDevices(@PathParam("regionId") String regionId,
                               InputStream stream) {
        RegionId rid = RegionId.regionId(regionId);
        Region region = nullIsNotFound(regionService.getRegion(rid),
                REGION_NOT_FOUND + rid);

        URI location;
        try {
            regionAdminService.addDevices(region.id(), extractDeviceIds(stream));
            location = new URI(rid.id());
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.created(location).build();
    }

    /**
     * Removes the specified collection of devices from the region.
     *
     * @param regionId region identifier
     * @param stream deviceIds JSON stream
     * @return 204 NO CONTENT
     * @onos.rsModel RegionDeviceIds
     */
    @DELETE
    @Path("{regionId}/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeDevices(@PathParam("regionId") String regionId,
                                  InputStream stream) {
        RegionId rid = RegionId.regionId(regionId);
        Region region = nullIsNotFound(regionService.getRegion(rid),
                REGION_NOT_FOUND + rid);

        try {
            regionAdminService.removeDevices(rid, extractDeviceIds(stream));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.noContent().build();
    }

    /**
     * Extracts device ids from a given JSON string.
     *
     * @param stream deviceIds JSON stream
     * @return a set of device identifiers
     * @throws IOException
     */
    private Set<DeviceId> extractDeviceIds(InputStream stream) throws IOException {
        ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
        JsonNode deviceIdsJson = jsonTree.get("deviceIds");

        if (deviceIdsJson == null || deviceIdsJson.size() == 0) {
            throw new IllegalArgumentException(DEVICE_IDS_INVALID);
        }

        Set<DeviceId> deviceIds = Sets.newHashSet();
        deviceIdsJson.forEach(did -> deviceIds.add(DeviceId.deviceId(did.asText())));

        return deviceIds;
    }
}
