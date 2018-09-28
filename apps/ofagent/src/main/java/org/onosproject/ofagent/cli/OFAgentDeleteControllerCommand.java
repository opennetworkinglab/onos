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
package org.onosproject.ofagent.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentAdminService;
import org.onosproject.ofagent.api.OFAgentService;
import org.onosproject.ofagent.impl.DefaultOFAgent;
import org.onosproject.ofagent.impl.DefaultOFController;

/**
 * Removes the controller from the OFAgent.
 */
@Service
@Command(scope = "onos", name = "ofagent-controller-delete",
        description = "Deletes a controller from the ofagent")
public class OFAgentDeleteControllerCommand extends AbstractShellCommand {

    private static final String PATTERN_IP_PORT = "\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d{1,5})";

    @Argument(index = 0, name = "network", description = "Virtual network ID",
            required = true, multiValued = false)
    private long networkId = NetworkId.NONE.id();

    @Argument(index = 1, name = "controller",
            description = "External controller with IP:PORT format",
            required = true, multiValued = false)
    private String strCtrl;

    @Override
    protected void doExecute() {
        if (!isValidController(strCtrl)) {
            error("Invalid controller string %s, must be IP:PORT", strCtrl);
            return;
        }

        OFAgentService service = get(OFAgentService.class);
        OFAgentAdminService adminService = get(OFAgentAdminService.class);

        OFAgent existing = service.agent(NetworkId.networkId(networkId));
        if (existing == null) {
            error("OFAgent for network %s does not exist", networkId);
            return;
        }

        String[] temp = strCtrl.split(":");
        OFAgent updated = DefaultOFAgent.builder()
                .from(existing)
                .deleteController(DefaultOFController.of(
                        IpAddress.valueOf(temp[0]),
                        TpPort.tpPort(Integer.valueOf(temp[1]))))
                .build();
        adminService.updateAgent(updated);
    }

    private boolean isValidController(String ctrl) {
        if (!ctrl.matches(PATTERN_IP_PORT)) {
            return false;
        }

        String[] temp = ctrl.split(":");
        try {
            IpAddress.valueOf(temp[0]);
            TpPort.tpPort(Integer.valueOf(temp[1]));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
