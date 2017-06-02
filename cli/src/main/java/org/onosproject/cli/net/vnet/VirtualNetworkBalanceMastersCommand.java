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
package org.onosproject.cli.net.vnet;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.mastership.MastershipAdminService;

/**
 * Forces virtual network device mastership rebalancing.
 */
@Command(scope = "onos", name = "vnet-balance-masters",
        description = "Forces virtual network device mastership rebalancing")
public class VirtualNetworkBalanceMastersCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;
    @Override
    protected void execute() {
        VirtualNetworkService vnetService = get(VirtualNetworkService.class);
        MastershipAdminService mastershipAdminService = vnetService
                .get(NetworkId.networkId(networkId), MastershipAdminService.class);
        mastershipAdminService.balanceRoles();
    }
}
