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
package org.onosproject.l2lb.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.l2lb.api.L2LbAdminService;
import org.onosproject.net.DeviceId;

/**
 * Command to remove a L2 load balancer.
 */
@Service
@Command(scope = "onos", name = "l2lb-remove", description = "Remove L2 load balancers ")
public class L2LbRemoveCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    private String deviceIdStr;

    @Argument(index = 1, name = "key",
            description = "L2 load balancer key",
            required = true, multiValued = false)
    private String keyStr;

    @Override
    protected void doExecute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        int l2LbPort = Integer.parseInt(keyStr);

        L2LbAdminService l2LbAdminService = get(L2LbAdminService.class);
        l2LbAdminService.remove(deviceId, l2LbPort);
    }
}
