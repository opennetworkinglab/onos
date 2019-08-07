/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.powermanagement;

import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;

import static org.onosproject.net.DeviceId.deviceId;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import static com.google.common.base.Preconditions.checkNotNull;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Manage inventory of infrastructure devices with Power Config behaviour.
 */
@Path("devices")
public class PowerConfigWebResource extends AbstractWebResource {

    private static final String JSON_INVALID = "Invalid json input";
    private static final String DEVICE_NOT_FOUND = "Device is not found";
    private static final String POWERCONFIG_UNSUPPORTED = "Power Config is not supported";
    private static final String DIRECTION_UNSUPPORTED = "Direction is not supported";

    private static final String DEVICES = "powerConfigDevices";
    private static final String PORTS = "ports";
    private static final String DEVICE_ID = "deviceId";
    private static final String DEVICE_IDS = "powerConfigDeviceIds";
    private static final String POWERCONFIG_SUPPORTED = "powerConfigSupported";
    private static final String DIRECTION = "direction";
    private static final String PORT_ID = "portId";
    private static final String TARGET_POWER = "targetPower";
    private static final String CURRENT_POWER = "currentPower";
    private static final String INPUT_POWER_RANGE = "inputPowerRange";
    private static final String TARGET_POWER_RANGE = "targetPowerRange";

    private final ObjectMapper mapper = new ObjectMapper();

    private static final Logger log = getLogger(PowerConfigWebResource.class);

    /**
     * Gets all power config devices.
     * Returns array of all discovered power config devices.
     *
     * @return 200 OK with a collection of devices
     * @onos.rsModel PowerConfigDevicesGet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevices() {
        ObjectNode root = mapper().createObjectNode();
        ArrayNode deviceIdsNode = root.putArray(DEVICE_IDS);

        Iterable<Device> devices = get(DeviceService.class).getDevices();
        if (devices != null) {
            for (Device d : devices) {
                if (getPowerConfig(d.id().toString()) != null) {
                    deviceIdsNode.add(d.id().toString());
                }
            }
        }

        return ok(root).build();
    }

    /**
     * Applies the target power for the specified device.
     *
     * @param stream JSON representation of device, port, component and target
     * power info
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel PowerConfigPut
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setTargetPower(InputStream stream) {
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            decode(jsonTree);
            return Response.ok().build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Gets the details of a power config device.
     * Returns the details of the specified power config device.
     *
     * @param id device identifier
     * @return 200 OK with a device
     * @onos.rsModel PowerConfigDeviceGet
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevice(@PathParam("id") String id) {
        ObjectNode result = mapper.createObjectNode();
        result.put(POWERCONFIG_SUPPORTED, (getPowerConfig(id) != null) ? true : false);
        return ok(result).build();
    }

    private PowerConfig<Object> getPowerConfig(String id) {
        Device device = get(DeviceService.class).getDevice(deviceId(id));
        if (device == null) {
            throw new IllegalArgumentException(DEVICE_NOT_FOUND);
        }
        if (device.is(PowerConfig.class)) {
            return device.as(PowerConfig.class);
        }
        return null;
    }

    /**
     * Gets the ports of a power config device.
     * Returns the details of the specified power config device ports.
     *
     * @onos.rsModel PowerConfigDeviceGetPorts
     * @param id device identifier
     * @param direction port direction
     * @param channel port channel
     * @return 200 OK with a collection of ports of the given device
     */
    @GET
    @Path("{id}/ports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDevicePorts(@PathParam("id") String id,
                                   @QueryParam("direction") String direction,
                                   @QueryParam("channel") String channel) {
        PowerConfig<Object> powerConfig = getPowerConfig(id);
        if (powerConfig == null) {
            throw new IllegalArgumentException(POWERCONFIG_UNSUPPORTED);
        }
        if (direction == null && channel == null) {
            direction = "ALL";
            // TODO: Fallback to all channels?
        }
        ObjectNode result = encode(powerConfig, direction, channel);
        return ok(result).build();
    }

    private ObjectNode encode(PowerConfig<Object> powerConfig, String direction, String channel) {
        checkNotNull(powerConfig, "PowerConfig cannot be null");
        ObjectNode powerConfigPorts = mapper.createObjectNode();
        Multimap<PortNumber, Object> portsMap = HashMultimap.create();

        if (direction != null) {
            for (PortNumber port : powerConfig.getPorts(direction)) {
                portsMap.put(port, Direction.valueOf(direction.toUpperCase()));
            }
        }

        if (channel != null) {
            for (PortNumber port : powerConfig.getPorts(channel)) {
                // TODO: channel to be handled
                portsMap.put(port, channel);
            }
        }

        for (Map.Entry<PortNumber, Object> entry : portsMap.entries()) {
            PortNumber port = entry.getKey();
            ObjectNode powerConfigComponents = mapper.createObjectNode();
            for (Object component : portsMap.get(port)) {
                // TODO: channel to be handled
                String componentName = "unknown";
                if (component instanceof Direction) {
                    componentName = component.toString();
                }
                ObjectNode powerConfigNode = mapper.createObjectNode()
                        .put(CURRENT_POWER, powerConfig.currentPower(port, component).orElse(0.0))
                        .put(TARGET_POWER, powerConfig.getTargetPower(port, component).orElse(0.0))
                        .put(INPUT_POWER_RANGE, powerConfig.getInputPowerRange(port,
                                component).orElse(Range.closed(0.0, 0.0)).toString())
                        .put(TARGET_POWER_RANGE, powerConfig.getTargetPowerRange(port,
                                component).orElse(Range.closed(0.0, 0.0)).toString());
                powerConfigComponents.set(componentName, powerConfigNode);
            }
            powerConfigPorts.set(port.toString(), powerConfigComponents);
        }

        ObjectNode result = mapper.createObjectNode();
        result.set("powerConfigPorts", powerConfigPorts);
        return result;
    }

    public void decode(ObjectNode json) {
        if (json == null || !json.isObject()) {
            throw new IllegalArgumentException(JSON_INVALID);
        }

        JsonNode devicesNode = json.get(DEVICES);
        if (!devicesNode.isObject()) {
            throw new IllegalArgumentException(JSON_INVALID);
        }

        Iterator<Entry<String, JsonNode>> deviceEntries = devicesNode.fields();
        while (deviceEntries.hasNext()) {
            Entry<String, JsonNode> deviceEntryNext = deviceEntries.next();
            String deviceId = deviceEntryNext.getKey();
            PowerConfig<Object> powerConfig = getPowerConfig(deviceId);
            JsonNode portsNode = deviceEntryNext.getValue();
            if (!portsNode.isObject()) {
                throw new IllegalArgumentException(JSON_INVALID);
            }

            Iterator<Entry<String, JsonNode>> portEntries = portsNode.fields();
            while (portEntries.hasNext()) {
                Entry<String, JsonNode> portEntryNext = portEntries.next();
                PortNumber portNumber = PortNumber.portNumber(portEntryNext.getKey());
                JsonNode componentsNode = portEntryNext.getValue();
                Iterator<Entry<String, JsonNode>> componentEntries = componentsNode.fields();
                while (componentEntries.hasNext()) {
                    Direction direction = null;
                    Entry<String, JsonNode> componentEntryNext = componentEntries.next();
                    try {
                        direction = Direction.valueOf(componentEntryNext.getKey().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // TODO: Handle other components
                    }

                    JsonNode powerNode = componentEntryNext.getValue();
                    if (!powerNode.isObject()) {
                        throw new IllegalArgumentException(JSON_INVALID);
                    }
                    Double targetPower = powerNode.get(TARGET_POWER).asDouble();
                    if (direction != null) {
                        powerConfig.setTargetPower(portNumber, direction, targetPower);
                    }
                }
            }
        }
    }
}
