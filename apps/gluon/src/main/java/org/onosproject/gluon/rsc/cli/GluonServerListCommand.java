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
package org.onosproject.gluon.rsc.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.gluon.manager.GluonManager;
import org.onosproject.gluon.rsc.GluonServer;

import java.util.Map;

import static org.onosproject.gluon.manager.GluonManager.getAllServersIP;
import static org.onosproject.gluon.rsc.GluonConstants.ACTIVE_SERVER;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_DEFAULT_PORT;
import static org.onosproject.gluon.rsc.GluonConstants.GLUON_HTTP;
import static org.onosproject.gluon.rsc.GluonConstants.SERVER_POOL;

/**
 * Supports for querying Gluon Servers list and statistics.
 */
@Service
@Command(scope = "onos", name = "gluon-server-list",
        description = "Gluon server list")
public class GluonServerListCommand extends AbstractShellCommand {

    @Option(name = "-i", aliases = "--server-ip",
            description = "Supports for querying Gluon server statistics",
            required = false, multiValued = false)
    String ipAddress = null;

    @Option(name = "-p", aliases = "--port", description = "Gluon server port",
            required = false, multiValued = false)
    String port = GLUON_DEFAULT_PORT;

    protected Map<String, GluonServer> serverMap = getAllServersIP();

    private static final String SERVER_STATISTICS =
            "Server %s details:\nVersion: %s\nPort: %s\nReal time data:\n" +
                    "\tSet Statistics   : %s\n\tDelete Statistics: %s\n" +
                    "Batch data:\n\tGet Statistics   : %s";


    @Override
    protected void doExecute() {
        try {
            String serverUrl = GLUON_HTTP + ipAddress + ":" + port;
            if (ipAddress != null && checkServerPool(serverUrl)) {
                for (Map.Entry<String,
                        GluonServer> server : serverMap.entrySet()) {

                    if (serverUrl.equals(server.getKey())) {
                        //Gets Etcd object reference
                        GluonServer gluonServer = server.getValue();
                        //Gets Etcd version from server list
                        print(SERVER_STATISTICS, ipAddress, gluonServer.version,
                              port, gluonServer.getSetCount(),
                              gluonServer.getDelCount(),
                              gluonServer.getGetCount());
                    }
                }
            } else {
                int totalServers = GluonManager.getTotalServers();
                log.info(ACTIVE_SERVER, totalServers);
                print("Number of active servers: " + totalServers);
                printServersIP();
            }
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }

    /**
     * Prints all servers IPs in table format.
     */
    protected void printServersIP() {
        int countServer = 1;
        for (Map.Entry<String, GluonServer> server : serverMap.entrySet()) {
            String serverUrl = server.getKey();
            String[] serverIP = serverUrl.split("//");
            print("Server %d: %s", countServer, serverIP[1]);
            countServer++;
        }
    }

    /**
     * Returns boolean if given IP available in server pool.
     *
     * @param ipAddr Ip Address
     * @return boolean
     */
    protected boolean checkServerPool(String ipAddr) {
        boolean isServerAvailable;
        if (serverMap.containsKey(ipAddr)) {
            isServerAvailable = true;
        } else {
            print(SERVER_POOL);
            isServerAvailable = false;
        }
        return isServerAvailable;
    }
}
