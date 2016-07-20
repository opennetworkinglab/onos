/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

import java.util.List;

import static org.onosproject.cli.net.DevicesListCommand.getSortedDevices;

/**
 * Lists mastership roles of nodes for each device.
 */
@Command(scope = "onos", name = "roles",
         description = "Lists mastership roles of nodes for each device.")
public class RolesCommand extends AbstractShellCommand {

    private static final String FMT_HDR = "%s: master=%s, standbys=[ %s]";

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        MastershipService roleService = get(MastershipService.class);

        if (outputJson()) {
            print("%s", json(roleService, getSortedDevices(deviceService)));
        } else {
            for (Device d : getSortedDevices(deviceService)) {
                DeviceId did = d.id();
                printRoles(roleService, did);
            }
        }
    }

    // Produces JSON structure with role information for the given devices.
    private JsonNode json(MastershipService service, List<Device> sortedDevices) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode results = mapper.createArrayNode();
        for (Device device : sortedDevices) {
            results.add(json(service, mapper, device));
        }
        return results;
    }

    // Produces JSON structure with role information for the given device.
    private JsonNode json(MastershipService service, ObjectMapper mapper,
                          Device device) {
        NodeId master = service.getMasterFor(device.id());
        ObjectNode result = mapper.createObjectNode()
                .put("id", device.id().toString())
                .put("master", master != null ? master.toString() : "none");
        RoleInfo nodes = service.getNodesFor(device.id());
        ArrayNode standbys = mapper.createArrayNode();
        for (NodeId nid : nodes.backups()) {
            standbys.add(nid.toString());
        }
        result.set("standbys", standbys);
        return result;
    }

    /**
     * Prints the role information for a device.
     *
     * @param service  mastership service
     * @param deviceId the ID of the device
     */
    protected void printRoles(MastershipService service, DeviceId deviceId) {
        RoleInfo nodes = service.getNodesFor(deviceId);
        StringBuilder builder = new StringBuilder();
        for (NodeId nid : nodes.backups()) {
            builder.append(nid).append(" ");
        }

        print(FMT_HDR, deviceId,
              nodes.master() == null ? "NONE" : nodes.master(),
              builder.toString());
    }
}
