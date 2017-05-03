/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping.web.api;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.mapping.MappingEntry;
import org.onosproject.mapping.MappingService;
import org.onosproject.mapping.MappingStore.Type;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * ONOS mapping management REST API implementation.
 */
@Path("mappings")
public class MappingsWebResource extends AbstractWebResource {

    private static final String DEVICE_NOT_FOUND = "Device is not found";
    private static final String TYPE_NOT_NULL = "Mapping store type should not be null";
    private static final String TYPE_ILLEGAL = "Mapping store type is not correct";
    private static final String MAPPINGS = "mappings";

    private static final String DB = "database";
    private static final String CACHE = "cache";

    private final MappingService mappingService = get(MappingService.class);
    private final DeviceService deviceService = get(DeviceService.class);
    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode mappingsNode = root.putArray(MAPPINGS);

    /**
     * Gets all mapping entries. Returns array of all mappings in the system.
     *
     * @param type mapping store type
     * @return 200 OK with a collection of mappings
     *
     * @onos.rsModel MappingEntries
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{type}")
    public Response getMappings(@PathParam("type") String type) {

        final Iterable<MappingEntry> mappingEntries =
                mappingService.getAllMappingEntries(getTypeEnum(type));

        if (mappingEntries == null || !mappingEntries.iterator().hasNext()) {
            return ok(root).build();
        }

        for (final MappingEntry entry : mappingEntries) {
            mappingsNode.add(codec(MappingEntry.class).encode(entry, this));
        }
        return ok(root).build();
    }

    /**
     * Gets mapping entries of a device. Returns array of all mappings for the
     * specified device.
     *
     * @param deviceId device identifier
     * @param type     mapping store type
     * @return 200 OK with a collection of mappings of given device
     *
     * @onos.rsModel MappingEntries
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{type}")
    public Response getMappingsByDeviceId(@PathParam("deviceId") String deviceId,
                                          @PathParam("type") String type) {
        Device device = deviceService.getDevice(DeviceId.deviceId(deviceId));

        if (device == null) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }

        final Iterable<MappingEntry> mappingEntries =
                mappingService.getMappingEntries(getTypeEnum(type), device.id());
        if (mappingEntries == null || !mappingEntries.iterator().hasNext()) {
            return ok(root).build();
        }

        for (final MappingEntry entry : mappingEntries) {
            mappingsNode.add(codec(MappingEntry.class).encode(entry, this));
        }
        return ok(root).build();
    }

    /**
     * Returns corresponding type enumeration based on the given
     * string formatted type.
     *
     * @param type string formatted type
     * @return type enumeration
     */
    private Type getTypeEnum(String type) {

        if (type == null) {
            throw new IllegalArgumentException(TYPE_NOT_NULL);
        }

        switch (type) {
            case DB:
                return Type.MAP_DATABASE;
            case CACHE:
                return Type.MAP_CACHE;
            default:
                throw new IllegalArgumentException(TYPE_ILLEGAL);
        }
    }
}
