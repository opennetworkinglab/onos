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

import org.onosproject.rest.AbstractWebResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.packet.PacketProcessorEntry;
import org.onosproject.net.packet.PacketService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.List;
import static org.onosproject.net.packet.PacketProcessor.ADVISOR_MAX;
import static org.onosproject.net.packet.PacketProcessor.DIRECTOR_MAX;

/**
 * Manage inventory of packet processors.
 */

@Path("packet/processors")
public class PacketProcessorsWebResource extends AbstractWebResource {

    /**
     * Gets packet processors. Returns array of all packet processors.

     * @onos.rsModel PacketProcessorsGet
     * @return 200 OK with array of all packet processors.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPacketProcessors() {
      PacketService service = get(PacketService.class);
      ObjectNode root = mapper().createObjectNode();
      ArrayNode pktProcNode = root.putArray("packet-processors");
      List<PacketProcessorEntry> processors = service.getProcessors();
      ObjectMapper mapper = new ObjectMapper();
      for (PacketProcessorEntry p : processors) {
            pktProcNode.add(mapper.createObjectNode()
                    .put("priority", priorityFormat(p.priority()))
                    .put("class", p.processor().getClass().getName())
                    .put("packets", p.invocations())
                    .put("avgNanos", p.averageNanos()));
      }

      return ok(root).build();
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
