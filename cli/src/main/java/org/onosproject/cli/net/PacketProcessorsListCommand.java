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
package org.onosproject.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.packet.PacketProcessorEntry;
import org.onosproject.net.packet.PacketService;

import java.util.List;

import static org.onosproject.net.packet.PacketProcessor.ADVISOR_MAX;
import static org.onosproject.net.packet.PacketProcessor.DIRECTOR_MAX;

/**
 * Lists packet processors.
 */
@Service
@Command(scope = "onos", name = "packet-processors",
        description = "Lists packet processors")
public class PacketProcessorsListCommand extends AbstractShellCommand {

    private static final String FMT = "priority=%s, class=%s, packets=%d, avgNanos=%d";

    @Override
    protected void doExecute() {
        PacketService service = get(PacketService.class);
        if (outputJson()) {
            print("%s", json(service.getProcessors()));
        } else {
            service.getProcessors().forEach(this::print);
        }
    }

    private JsonNode json(List<PacketProcessorEntry> processors) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (PacketProcessorEntry p : processors) {
            result.add(mapper.createObjectNode()
                    .put("priority", priorityFormat(p.priority()))
                    .put("class", p.processor().getClass().getName())
                    .put("packets", p.invocations())
                    .put("avgNanos", p.averageNanos()));
        }

        return result;
    }

    private void print(PacketProcessorEntry entry) {
        print(FMT, priorityFormat(entry.priority()),
              entry.processor().getClass().getName(),
              entry.invocations(), entry.averageNanos());
    }

    private String priorityFormat(int priority) {
        if (priority > DIRECTOR_MAX) {
            return "observer(" + (priority - DIRECTOR_MAX - 1) + ")";
        } else if (priority > ADVISOR_MAX) {
            return "director(" + (priority - ADVISOR_MAX - 1) + ")";
        }
        return "advisor(" + (priority - 1) + ")";
    }

}
