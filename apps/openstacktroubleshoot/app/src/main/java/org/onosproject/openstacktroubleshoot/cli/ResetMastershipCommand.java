/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.openstacktroubleshoot.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.net.Device;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceService;

/**
 * Re-configure mastership.
 */
@Service
@Command(scope = "onos", name = "openstack-reset-mastership",
        description = "Reconfigure the mastership")
public class ResetMastershipCommand extends AbstractShellCommand {

    @Option(name = "-c", aliases = "--concentrate",
            description = "enforce all switches to move to one controller",
            required = false, multiValued = false)
    private boolean isConcentrate = false;

    @Option(name = "-b", aliases = "--balance",
            description = "enforce all switches to be evenly distributed",
            required = false, multiValued = false)
    private boolean isBalance = false;

    @Override
    protected void doExecute() {
        MastershipAdminService mastershipService = get(MastershipAdminService.class);
        ClusterService clusterService = get(ClusterService.class);
        DeviceService deviceService = get(DeviceService.class);

        if ((isConcentrate && isBalance) || (!isConcentrate && !isBalance)) {
            print("Please specify either -b or -c option only");
            return;
        }

        NodeId localId = clusterService.getLocalNode().id();

        if (isConcentrate) {
            deviceService.getAvailableDevices(Device.Type.SWITCH).forEach(d ->
                    mastershipService.setRole(localId, d.id(), MastershipRole.MASTER));
        }

        if (isBalance) {
            mastershipService.balanceRoles();
        }
    }
}
