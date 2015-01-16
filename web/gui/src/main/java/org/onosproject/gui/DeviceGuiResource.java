/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.rest.BaseResource;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * UI REST resource for interacting with the inventory of infrastructure devices.
 */
@Path("device")
public class DeviceGuiResource extends BaseResource {

    private static final String ICON_ID_ONLINE = "deviceOnline";
    private static final String ICON_ID_OFFLINE = "deviceOffline";

    private static final Logger log = getLogger(DeviceGuiResource.class);

    private final ObjectMapper mapper = new ObjectMapper();


    // return list of devices
    @GET
    @Produces("application/json")
    public Response getDevices() {
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode devices = mapper.createArrayNode();
        DeviceService service = get(DeviceService.class);

        for (Device dev: service.getDevices()) {
            devices.add(deviceJson(service, dev));
        }

        rootNode.set("devices", devices);
        return Response.ok(rootNode.toString()).build();
    }

    /**
     * Returns a JSON node representing the specified device.
     *
     * @param device infrastructure device
     * @return JSON node
     */
    private ObjectNode deviceJson(DeviceService service, Device device) {
        boolean available = service.isAvailable(device.id());
        // pick the appropriate id for the icon to appear in the table row
        String iconId = available ? ICON_ID_ONLINE : ICON_ID_OFFLINE;

        ObjectNode result = mapper.createObjectNode();
        result.put("id", device.id().toString())
                .put("available", available)
                .put("_iconid_available", iconId)
                .put("type", device.type().toString())
                .put("role", service.getRole(device.id()).toString())
                .put("mfr", device.manufacturer())
                .put("hw", device.hwVersion())
                .put("sw", device.swVersion())
                .put("serial", device.serialNumber())
                .set("annotations", annotations(mapper, device.annotations()));
        return result;
    }

    /**
     * Produces a JSON object from the specified key/value annotations.
     *
     * @param mapper ObjectMapper to use while converting to JSON
     * @param annotations key/value annotations
     * @return JSON object
     */
    private static ObjectNode annotations(ObjectMapper mapper, Annotations annotations) {
        ObjectNode result = mapper.createObjectNode();
        for (String key : annotations.keys()) {
            result.put(key, annotations.value(key));
        }
        return result;
    }

}
