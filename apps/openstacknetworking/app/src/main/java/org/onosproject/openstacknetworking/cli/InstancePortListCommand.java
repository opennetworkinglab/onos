/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;

import java.util.Comparator;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * Lists OpenStack instance ports.
 */
@Command(scope = "onos", name = "openstack-instance-ports",
        description = "Lists all OpenStack instance ports")
public class InstancePortListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-10s%-25s%-15s%-20s";

    @Override
    protected void execute() {
        InstancePortService service = get(InstancePortService.class);
        List<InstancePort> instancePorts = Lists.newArrayList(service.instancePorts());
        instancePorts.sort(Comparator.comparing(InstancePort::portId));

        if (outputJson()) {
            print("%s", json(this, instancePorts));
        } else {
            print(FORMAT, "ID", "State", "Device ID", "Port Number", "Fixed IP");
            for (InstancePort port : instancePorts) {
                print(FORMAT, port.portId(), port.state(), port.deviceId().toString(),
                        port.portNumber().toLong(), port.ipAddress().toString());
            }
        }
    }

    private JsonNode json(AbstractShellCommand context, List<InstancePort> ports) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.enable(INDENT_OUTPUT).createArrayNode();
        ports.forEach(p -> result.add(context.jsonForEntity(p, InstancePort.class)));

        return result;
    }
}
