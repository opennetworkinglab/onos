/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 * This Work is contributed by Sterlite Technologies
 */
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.Device;
import org.onosproject.net.Direction;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ModulationConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.onosproject.net.DeviceId.deviceId;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query and program flow rules.
 */

@Path("modulation")
public class ModulationWebResource extends AbstractWebResource {

    private final Logger log = getLogger(getClass());
    @Context
    private UriInfo uriInfo;

    private static final String DEVICE_NOT_FOUND = "Device is not found";
    private static final String APP_ID_NOT_FOUND = "Application Id is not found";
    private static final String MODULATIONCONFIG_UNSUPPORTED = "Modulation Config is not supported";

    private static final String DEVICES = "modulationConfigDevices";
    private static final String DEVICE_ID = "deviceId";
    private static final String DEVICE_IDS = "modulationConfigDeviceIds";
    private static final String MODULATIONCONFIG_SUPPORTED = "modulationConfigSupported";
    private static final String TARGET_MODULATION = "targetModulation";
    private static final String TARGET_BITRATE = "targetBitRate";
    private static final String JSON_INVALID = "Invalid json input";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Gets all modulation config devices.
     * Returns array of all discovered modulation config devices.
     *
     * @return 200 OK with a collection of devices supporting modulation
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getModulationSupportedDevices() {
        ObjectNode root = mapper().createObjectNode();
        ArrayNode deviceIdsNode = root.putArray(DEVICE_IDS);

        Iterable<Device> devices = get(DeviceService.class).getDevices();
        if (devices != null) {
            for (Device d : devices) {
                if (getModulationConfig(d.id().toString()) != null) {
                    deviceIdsNode.add(d.id().toString());
                }
            }
        }

        return ok(root).build();
    }

    private ModulationConfig<Object> getModulationConfig(String id) {
        Device device = get(DeviceService.class).getDevice(deviceId(id));
        if (device == null) {
            throw new IllegalArgumentException(DEVICE_NOT_FOUND);
        }
        if (device.is(ModulationConfig.class)) {
            return device.as(ModulationConfig.class);
        }
        return null;
    }


    /**
     * Gets the details of a modulation config device.
     * Returns the details of the specified modulation config device.
     *
     * @param id device identifier
     * @return 200 OK with a device
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response isModulationSupported(@PathParam("id") String id) {
        ObjectNode result = mapper.createObjectNode();
        result.put(MODULATIONCONFIG_SUPPORTED, (getModulationConfig(id) != null) ? true : false);
        return ok(result).build();
    }


    /**
     * Returns the supported modulation scheme for specified ports of device.
     * <p>
     * ModulationConfigDeviceGetPorts
     *
     * @param id     device identifier
     * @param portId line port identifier
     * @return 200 OK with a modulationGetResponse.
     */
    @GET
    @Path("{id}/port")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getConfigurablePortModulation(@PathParam("id") String id,
                                                  @QueryParam("port_id") String portId) {
        ModulationConfig<Object> modulationConfig = getModulationConfig(id);
        if (modulationConfig == null) {
            throw new IllegalArgumentException(MODULATIONCONFIG_UNSUPPORTED);
        }

        ObjectNode result = encode(id, modulationConfig, portId);
        return ok(result).build();
    }


    /**
     * Applies the target modulation for the specified device.
     *
     * @param stream JSON representation of device, port, component and target
     *               bitrate info
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * ModulationConfigPut
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setModulationScheme(InputStream stream) {
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            decode(jsonTree);
            return Response.ok().build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private ObjectNode encode(String deviceId, ModulationConfig<Object> modulationConfig, String portId) {
        checkNotNull(modulationConfig, "ModulationConfig cannot be null");

        Direction component = Direction.ALL;
        PortNumber port = PortNumber.portNumber(portId);
        log.debug("Port Details for port_id : {} is \n {} ", portId, port);

        ModulationScheme modulation = modulationConfig.getModulationScheme(port, component).get();
        log.debug("Modulation  fetched from driver : " + modulation.name());

        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("deviceId", deviceId);
        responseNode.put("portId", portId);
        responseNode.put("modulation", modulation.name());

        return responseNode;
    }

    public void decode(ObjectNode json) {
        if (json == null || !json.isObject()) {
            throw new IllegalArgumentException(JSON_INVALID);
        }

        JsonNode devicesNode = json.get(DEVICES);
        if (!devicesNode.isObject()) {
            throw new IllegalArgumentException(JSON_INVALID);
        }

        Iterator<Map.Entry<String, JsonNode>> deviceEntries = devicesNode.fields();
        while (deviceEntries.hasNext()) {
            Map.Entry<String, JsonNode> deviceEntryNext = deviceEntries.next();
            String deviceId = deviceEntryNext.getKey();
            ModulationConfig<Object> modulationConfig = getModulationConfig(deviceId);
            JsonNode portsNode = deviceEntryNext.getValue();
            if (!portsNode.isObject()) {
                throw new IllegalArgumentException(JSON_INVALID);
            }

            Iterator<Map.Entry<String, JsonNode>> portEntries = portsNode.fields();
            while (portEntries.hasNext()) {
                Map.Entry<String, JsonNode> portEntryNext = portEntries.next();
                PortNumber portNumber = PortNumber.portNumber(portEntryNext.getKey());
                JsonNode componentsNode = portEntryNext.getValue();
                Iterator<Map.Entry<String, JsonNode>> componentEntries = componentsNode.fields();
                while (componentEntries.hasNext()) {
                    Direction direction = null;
                    Map.Entry<String, JsonNode> componentEntryNext = componentEntries.next();
                    try {
                        direction = Direction.valueOf(componentEntryNext.getKey().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // TODO: Handle other components
                    }

                    JsonNode bitRateNode = componentEntryNext.getValue();
                    if (!bitRateNode.isObject()) {
                        throw new IllegalArgumentException(JSON_INVALID);
                    }
                    Long targetBitRate = bitRateNode.get(TARGET_BITRATE).asLong();
                    if (direction != null) {
                        modulationConfig.setModulationScheme(portNumber, direction, targetBitRate);
                    }
                }
            }
        }
    }

    /**
     * Sets the modulation for specified device and port.
     * Returns the details of the specified modulation config device ports.
     * <p>
     * ModulationConfigDeviceGetPorts
     *
     * @param id        device identifier
     * @param direction port direction(transmitter or receiver port)
     * @param portId    port channel
     * @param bitrate   port bitrate
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @PUT
    @Path("set-modulation/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response setPortModulation(@PathParam("id") String id,
                                      @QueryParam("port_id") String portId,
                                      @QueryParam("direction") String direction,
                                      @QueryParam("bitrate") long bitrate) {

        ModulationConfig<Object> modulationConfig = getModulationConfig(id);
        PortNumber portNumber = PortNumber.portNumber(portId);
        log.info("Port Details for port_id : {} is \n {} ", portId, portNumber);
        if (direction != null) {
            Direction component = Direction.valueOf(direction.toUpperCase());
            modulationConfig.setModulationScheme(portNumber, component, bitrate);
        } else {
            log.error("Direction cannot be null");
            return Response.status(400, "Direction cannot be Null").build();
        }
        return Response.ok().build();
    }


}
