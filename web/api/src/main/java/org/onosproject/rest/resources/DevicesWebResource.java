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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Manage inventory of infrastructure devices.
 */
@Path("devices")
public class DevicesWebResource extends AbstractWebResource {

    private static final String ENABLED = "enabled";

    private static final String DEVICE_NOT_FOUND = "Device is not found";
    private static final String INVALID_JSON = "Invalid JSON data";

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
      * Gets ports of all infrastructure devices.
      * Returns port details of all infrastructure devices.
      *
      * @onos.rsModel DevicesGetPorts
      * @return 200 OK with a collection of ports for all devices
      */
    @GET
    @Path("ports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicesPorts() {
        DeviceService service = get(DeviceService.class);
        List<Port> result = Lists.newArrayList();
        service.getDevices().forEach(device -> {
                Optional<List<Port>> list = Optional.ofNullable(service.getPorts(device.id()));
                list.ifPresent(ports -> result.addAll(ports));
        });
        return ok(encodeArray(Port.class, "ports", result)).build();
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

    /**
     * Changes the administrative state of a port.
     *
     * @onos.rsModel PortAdministrativeState
     * @param id device identifier
     * @param portId port number
     * @param stream input JSON
     * @return 200 OK if the port state was set to the given value
     */
    @POST
    @Path("{id}/portstate/{port_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response setPortState(@PathParam("id") String id,
                                 @PathParam("port_id") String portId,
                                 InputStream stream) {
        try {
            DeviceId deviceId = deviceId(id);
            PortNumber portNumber = PortNumber.portNumber(portId);
            nullIsNotFound(get(DeviceService.class).getPort(
                    new ConnectPoint(deviceId, portNumber)), DEVICE_NOT_FOUND);

            ObjectNode root = readTreeFromStream(mapper(), stream);
            JsonNode node = root.path(ENABLED);

            if (!node.isMissingNode()) {
                get(DeviceAdminService.class)
                        .changePortState(deviceId, portNumber, node.asBoolean());
                return Response.ok().build();
            }

            throw new IllegalArgumentException(INVALID_JSON);
        } catch (IOException ioe) {
            throw new IllegalArgumentException(ioe);
        }
    }

}
