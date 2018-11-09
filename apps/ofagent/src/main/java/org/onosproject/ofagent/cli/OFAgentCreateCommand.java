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

import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentAdminService;
import org.onosproject.ofagent.api.OFController;
import org.onosproject.ofagent.impl.DefaultOFAgent;
import org.onosproject.ofagent.impl.DefaultOFController;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Creates a new OFAagent.
 */
@Service
@Command(scope = "onos", name = "ofagent-create", description = "Add a new ofagent")
public class OFAgentCreateCommand extends AbstractShellCommand {

    private static final String PATTERN_IP_PORT = "\\d{1,3}(?:\\.\\d{1,3}){3}(?::\\d{1,5})";

    @Argument(index = 0, name = "network", description = "Virtual network ID",
            required = true, multiValued = false)
    private long networkId = NetworkId.NONE.id();

    @Argument(index = 1, name = "controllers",
            description = "List of external controllers with IP:PORT format",
            required = false, multiValued = true)
    private String[] strCtrls = {};

    @Override
    protected void doExecute() {
        Set<OFController> ctrls = Sets.newHashSet();
        for (String strCtrl : strCtrls) {
            if (!isValidController(strCtrl)) {
                print("Invalid controller %s, ignores it.", strCtrl);
                continue;
            }
            String[] temp = strCtrl.split(":");
            ctrls.add(DefaultOFController.of(IpAddress.valueOf(temp[0]),
                    TpPort.tpPort(Integer.valueOf(temp[1]))));
        }

        VirtualNetworkService virtualNetworkService = get(VirtualNetworkService.class);
        TenantId tenantId = virtualNetworkService.getTenantId(NetworkId.networkId(networkId));
        checkNotNull(tenantId, "Virtual network %s does not have tenant.", networkId);
        OFAgentAdminService adminService = get(OFAgentAdminService.class);
        OFAgent ofAgent = DefaultOFAgent.builder()
                .networkId(NetworkId.networkId(networkId))
                .tenantId(tenantId)
                .controllers(ctrls)
                .state(OFAgent.State.STOPPED)
                .build();
        adminService.createAgent(ofAgent);
        print("Successfully created OFAgent for network %s, tenant %s", networkId, tenantId);
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
