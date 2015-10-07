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
package org.onosproject.vtnrsc.cli.network;

import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.DefaultTenantNetwork;
import org.onosproject.vtnrsc.PhysicalNetwork;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;

import com.google.common.collect.Sets;

/**
 * Supports for creating a TenantNetwork.
 */
@Command(scope = "onos", name = "tenantnetwork-create",
        description = "Supports for creating a TenantNetwork")
public class TenantNetworkCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "TenantNetwork network id", required = true,
            multiValued = false)
    String id = null;

    @Argument(index = 1, name = "tenantID", description = "The tenant id of TenantNetwork",
            required = true, multiValued = false)
    String tenantID = null;

    @Argument(index = 2, name = "type", description = "The type of TenantNetwork", required = true,
            multiValued = false)
    String type = null;

    @Argument(index = 3, name = "segmentationID", description = "The segmentation id of TenantNetwork",
            required = true, multiValued = false)
    String segmentationID = "";

    @Option(name = "-n", aliases = "--name", description = "TenantNetwork name", required = false,
            multiValued = false)
    String name = null;

    @Option(name = "-a", aliases = "--adminStateUp", description = "TenantNetwork adminStateUp is true or false",
            required = false, multiValued = false)
    boolean adminStateUp = false;

    @Option(name = "-s", aliases = "--state", description = "The state of TenantNetwork",
            required = false, multiValued = false)
    String state = null;

    @Option(name = "-d", aliases = "--shared", description = "TenantNetwork is shared or not",
            required = false, multiValued = false)
    boolean shared = false;

    @Option(name = "-r", aliases = "--routerExternal",
            description = "TenantNetwork is routerExternal or not", required = false,
            multiValued = false)
    boolean routerExternal = false;

    @Option(name = "-p", aliases = "--physicalNetwork", description = "The physical network of Tenant",
            required = false, multiValued = false)
    String physicalNetwork = "";

    @Override
    protected void execute() {
        TenantNetworkService service = get(TenantNetworkService.class);
        TenantNetwork network = new DefaultTenantNetwork(TenantNetworkId.networkId(id), name,
                                                         adminStateUp,
                                                         TenantNetwork.State.valueOf(state),
                                                         shared, TenantId.tenantId(tenantID),
                                                         routerExternal,
                                                         TenantNetwork.Type.valueOf(type),
                                                         PhysicalNetwork.physicalNetwork(physicalNetwork),
                                                         SegmentationId.segmentationId(segmentationID));

        Set<TenantNetwork> networksSet = Sets.newHashSet(network);
        service.createNetworks(networksSet);
    }
}
