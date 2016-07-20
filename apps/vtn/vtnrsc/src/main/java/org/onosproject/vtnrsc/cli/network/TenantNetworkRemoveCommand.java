/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.cli.network;

import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;

import com.google.common.collect.Sets;

/**
 * Supports for removing a TenantNetwork by network id.
 */
@Command(scope = "onos", name = "tenantnetwork-remove", description = "Supports for removing"
        + " a tenantNetwork by tenantNetworkid")
public class TenantNetworkRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "TenantNetwork neutronNetwork Id",
            required = true, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        TenantNetworkService service = get(TenantNetworkService.class);
        Set<TenantNetworkId> networkIds = Sets.newHashSet(TenantNetworkId.networkId(id));
        service.removeNetworks(networkIds);
    }
}
