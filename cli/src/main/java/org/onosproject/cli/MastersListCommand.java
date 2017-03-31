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
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.utils.Comparators;

import java.util.Collections;
import java.util.List;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists device mastership information.
 */
@Command(scope = "onos", name = "masters",
         description = "Lists device mastership information")
public class MastersListCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        ClusterService service = get(ClusterService.class);
        MastershipService mastershipService = get(MastershipService.class);
        DeviceService deviceService = get(DeviceService.class);
        List<ControllerNode> nodes = newArrayList(service.getNodes());
        Collections.sort(nodes, Comparators.NODE_COMPARATOR);

        if (outputJson()) {
            print("%s", json(service, mastershipService, nodes));
        } else {
            for (ControllerNode node : nodes) {
                List<DeviceId> ids = Lists.newArrayList(mastershipService.getDevicesOf(node.id()));
                ids.removeIf(did -> deviceService.getDevice(did) == null);
                Collections.sort(ids, Comparators.ELEMENT_ID_COMPARATOR);
                print("%s: %d devices", node.id(), ids.size());
                for (DeviceId deviceId : ids) {
                    print("  %s", deviceId);
                }
            }
        }
    }

    // Produces JSON structure.
    private JsonNode json(ClusterService service, MastershipService mastershipService,
                          List<ControllerNode> nodes) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (ControllerNode node : nodes) {
            List<DeviceId> ids = Lists.newArrayList(mastershipService.getDevicesOf(node.id()));
            result.add(mapper.createObjectNode()
                               .put("id", node.id().toString())
                               .put("size", ids.size())
                               .set("devices", json(mapper, ids)));
        }
        return result;
    }

    /**
     * Produces a JSON array containing the specified device identifiers.
     *
     * @param mapper object mapper
     * @param ids    collection of device identifiers
     * @return JSON array
     */
    public static JsonNode json(ObjectMapper mapper, Iterable<DeviceId> ids) {
        ArrayNode result = mapper.createArrayNode();
        for (DeviceId deviceId : ids) {
            result.add(deviceId.toString());
        }
        return result;
    }

}
