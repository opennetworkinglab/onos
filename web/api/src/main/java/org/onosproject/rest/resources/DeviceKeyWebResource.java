/*
 * Copyright 2016-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyService;
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

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Query and Manage Device Keys.
 */
@Path("keys")
public class DeviceKeyWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private static final String DEVICE_KEY_NOT_FOUND = "Device key was not found";

    /**
     * Gets all device keys.
     * Returns array of all device keys.
     *
     * @return 200 OK with a collection of device keys
     * @onos.rsModel Devicekeys
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceKeys() {
        Iterable<DeviceKey> deviceKeys = get(DeviceKeyService.class).getDeviceKeys();
        return ok(encodeArray(DeviceKey.class, "keys", deviceKeys)).build();
    }

    /**
     * Gets a single device key by device key unique identifier.
     * Returns the specified device key.
     *
     * @param id device key identifier
     * @return 200 OK with a device key, 404 not found
     * @onos.rsModel Devicekey
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeviceKey(@PathParam("id") String id) {
        DeviceKey deviceKey = nullIsNotFound(get(DeviceKeyService.class).getDeviceKey(DeviceKeyId.deviceKeyId(id)),
                                             DEVICE_KEY_NOT_FOUND);
        return ok(codec(DeviceKey.class).encode(deviceKey, this)).build();
    }

    /**
     * Adds a new device key from the JSON input stream.
     *
     * @param stream device key JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel Devicekey
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDeviceKey(InputStream stream) {
        try {
            DeviceKeyAdminService service = get(DeviceKeyAdminService.class);
            ObjectNode root = readTreeFromStream(mapper(), stream);
            DeviceKey deviceKey = codec(DeviceKey.class).decode(root, this);
            service.addKey(deviceKey);

            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("keys")
                    .path(deviceKey.deviceKeyId().id());

            return Response
                    .created(locationBuilder.build())
                    .build();

        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

    /**
     * Removes a device key by device key identifier.
     *
     * @param id device key identifier
     * @return 200 OK with a removed device key, 404 not found
     */
    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeDeviceKey(@PathParam("id") String id) {
        DeviceKey deviceKey = nullIsNotFound(get(DeviceKeyService.class).getDeviceKey(DeviceKeyId.deviceKeyId(id)),
                                             DEVICE_KEY_NOT_FOUND);
        get(DeviceKeyAdminService.class).removeKey(DeviceKeyId.deviceKeyId(id));
        return ok(codec(DeviceKey.class).encode(deviceKey, this)).build();
    }
}
