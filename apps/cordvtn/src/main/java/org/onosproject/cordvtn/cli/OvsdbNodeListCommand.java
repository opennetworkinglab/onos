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

package org.onosproject.cordvtn.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cordvtn.CordVtnService;
import org.onosproject.cordvtn.OvsdbNode;

import java.util.Collections;
import java.util.List;

/**
 * Lists all OVSDB nodes.
 */
@Command(scope = "onos", name = "ovsdbs",
        description = "Lists all OVSDB nodes registered in cordvtn application")
public class OvsdbNodeListCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        CordVtnService service = AbstractShellCommand.get(CordVtnService.class);
        List<OvsdbNode> ovsdbs = service.getNodes();
        Collections.sort(ovsdbs, OvsdbNode.OVSDB_NODE_COMPARATOR);

        if (outputJson()) {
            print("%s", json(service, ovsdbs));
        } else {
            for (OvsdbNode ovsdb : ovsdbs) {
                print("host=%s, address=%s, br-int=%s, state=%s",
                      ovsdb.host(),
                      ovsdb.ip().toString() + ":" + ovsdb.port().toString(),
                      ovsdb.intBrId().toString(),
                      getState(service, ovsdb));
            }
            print("Total %s nodes", service.getNodeCount());
        }
    }

    private JsonNode json(CordVtnService service, List<OvsdbNode> ovsdbs) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (OvsdbNode ovsdb : ovsdbs) {
            String ipPort = ovsdb.ip().toString() + ":" + ovsdb.port().toString();
            result.add(mapper.createObjectNode()
                               .put("host", ovsdb.host())
                               .put("address", ipPort)
                               .put("brInt", ovsdb.intBrId().toString())
                               .put("state", getState(service, ovsdb)));
        }
        return result;
    }

    private String getState(CordVtnService service, OvsdbNode ovsdb) {
        return service.isNodeConnected(ovsdb) ? "CONNECTED" : "DISCONNECTED";
    }
}
