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
package org.onosproject.portloadbalancer.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.portloadbalancer.api.PortLoadBalancer;
import org.onosproject.portloadbalancer.api.PortLoadBalancerAdminService;
import org.onosproject.portloadbalancer.api.PortLoadBalancerId;
import org.onosproject.net.DeviceId;

/**
 * Command to remove a port load balancer.
 */
@Service
@Command(scope = "onos", name = "plb-remove", description = "Remove port load balancers ")
public class PortLoadBalancerRemoveCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    private String deviceIdStr;

    @Argument(index = 1, name = "key",
            description = "port load balancer key",
            required = true, multiValued = false)
    private String keyStr;

    // Operation constants
    private static final String EXECUTED = "Executed";
    private static final String FAILED = "Failed";

    @Override
    protected void doExecute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        int portLoadBalancerKey = Integer.parseInt(keyStr);

        PortLoadBalancerAdminService portLoadBalancerAdminService = get(PortLoadBalancerAdminService.class);
        PortLoadBalancerId portLoadBalancerId = new PortLoadBalancerId(deviceId, portLoadBalancerKey);
        PortLoadBalancer portLoadBalancer = portLoadBalancerAdminService.remove(portLoadBalancerId);
        print("Removal of %s %s", portLoadBalancerId, portLoadBalancer != null ? EXECUTED : FAILED);
    }
}
