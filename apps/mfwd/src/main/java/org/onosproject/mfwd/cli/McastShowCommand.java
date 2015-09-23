/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.mfwd.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onlab.packet.IpPrefix;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.onosproject.mfwd.impl.McastRouteTable;
import org.onosproject.mfwd.impl.McastRouteGroup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Displays the source, multicast group flows entries.
 */
@Command(scope = "onos", name = "mcast-show", description = "Displays the source, multicast group flows")
public class McastShowCommand extends AbstractShellCommand {

    private final Logger log = getLogger(getClass());

    @Override
    protected void execute() {
        McastRouteTable mrt = McastRouteTable.getInstance();
        if (outputJson()) {
            print("%s", json(mrt));
        } else {
            printMrib4(mrt);
        }
    }

    public JsonNode json(McastRouteTable mrt) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        Map<IpPrefix, McastRouteGroup> mrib4 = mrt.getMrib4();
        for (McastRouteGroup mg : mrib4.values()) {
            String sAddr = "";
            String gAddr = "";
            String inPort = "";
            String outPorts = "";
            if (mg.getSaddr() != null) {
                sAddr = mg.getSaddr().toString();
                log.info("Multicast Source: " + sAddr);
            }
            if (mg.getGaddr() != null) {
                gAddr = mg.getGaddr().toString();
                log.info("Multicast Group: " + gAddr);
            }
            if (mg.getIngressPoint() != null) {
                inPort = mg.getIngressPoint().toString();
                log.info("Multicast Ingress: " + inPort);
            }
            Set<ConnectPoint> eps = mg.getEgressPoints();
            if (eps != null && !eps.isEmpty()) {
                outPorts = eps.toString();
            }
            result.add(mapper.createObjectNode()
                                    .put("src", sAddr)
                                    .put("grp", gAddr)
                                    .put("inPort", inPort)
                                    .put("outPorts", outPorts));
        }
        return result;
    }

    /**
     * Displays multicast route table entries.
     *
     * @param mrt route table
     */
    protected void printMrib4(McastRouteTable mrt) {
        print(mrt.printMcastRouteTable());
    }
}
