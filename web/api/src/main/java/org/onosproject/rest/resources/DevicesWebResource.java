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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Manage inventory of infrastructure devices.
 */
@Path("devices")
public class DevicesWebResource extends AbstractWebResource {

    private static final String DEVICE_NOT_FOUND = "Device is not found";

    /**
     * Gets all infrastructure devices.
     * Returns array of all discovered infrastructure devices.
     *
     * @return 200 OK with a collection of devices
     * @onos.rsModel DevicesGet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices() {
        Iterable<Device> devices = get(DeviceService.class).getDevices();
        return ok(encodeArray(Device.class, "devices", devices)).build();
    }

    /**
     * Gets details of infrastructure device.
     * Returns details of the specified infrastructure device.
     *
     * @param id device identifier
     * @return 200 OK with a device
     * @onos.rsModel DeviceGet
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevice(@PathParam("id") String id) {
        Device device = nullIsNotFound(get(DeviceService.class).getDevice(deviceId(id)),
                                       DEVICE_NOT_FOUND);
        return ok(codec(Device.class).encode(device, this)).build();
    }

    /**
     * Removes infrastructure device.
     * Administratively deletes the specified device from the inventory of
     * known devices.
     *
     * @param id device identifier
     * @return 200 OK with the removed device
     */
    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeDevice(@PathParam("id") String id) {
        Device device = nullIsNotFound(get(DeviceService.class).getDevice(deviceId(id)),
                                       DEVICE_NOT_FOUND);
        ObjectNode result = codec(Device.class).encode(device, this);
        get(DeviceAdminService.class).removeDevice(deviceId(id));
        return ok(result).build();
    }

    /**
     * Gets ports of infrastructure device.
     * Returns details of the specified infrastructure device.
     *
     * @onos.rsModel DeviceGetPorts
     * @param id device identifier
     * @return 200 OK with a collection of ports of the given device
     */
    @GET
    @Path("{id}/ports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicePorts(@PathParam("id") String id) {
        DeviceService service = get(DeviceService.class);
        Device device = nullIsNotFound(service.getDevice(deviceId(id)), DEVICE_NOT_FOUND);
        List<Port> ports = checkNotNull(service.getPorts(deviceId(id)), "Ports could not be retrieved");
        ObjectNode result = codec(Device.class).encode(device, this);
        result.set("ports", codec(Port.class).encode(ports, this));
        return ok(result).build();
    }

}
