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

import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.portloadbalancer.api.PortLoadBalancer;
import org.onosproject.portloadbalancer.api.PortLoadBalancerAdminService;
import org.onosproject.portloadbalancer.api.PortLoadBalancerId;
import org.onosproject.portloadbalancer.api.PortLoadBalancerMode;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Command to add a port load balancer.
 */
@Service
@Command(scope = "onos", name = "plb-add", description = "Create or update port load balancer ")
public class PortLoadBalancerAddCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    private String deviceIdStr;

    @Argument(index = 1, name = "key",
            description = "port load balancer key",
            required = true, multiValued = false)
    private String keyStr;

    @Argument(index = 2, name = "mode",
            description = "port load balancer mode. STATIC or LACP",
            required = true, multiValued = false)
    private String modeStr;

    @Argument(index = 3, name = "ports",
            description = "port load balancer physical ports",
            required = true, multiValued = true)
    private String[] portsStr;

    // Operation constants
    private static final String CREATE = "Create";
    private static final String UPDATE = "Update";

    @Override
    protected void doExecute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        int portLoadBalancerKey = Integer.parseInt(keyStr);

        PortLoadBalancerMode mode = PortLoadBalancerMode.valueOf(modeStr.toUpperCase());
        Set<PortNumber> ports = Sets.newHashSet(portsStr).stream()
                .map(PortNumber::fromString).collect(Collectors.toSet());

        PortLoadBalancerAdminService portLoadBalancerAdminService = get(PortLoadBalancerAdminService.class);
        PortLoadBalancerId portLoadBalancerId = new PortLoadBalancerId(deviceId, portLoadBalancerKey);
        PortLoadBalancer portLoadBalancer = portLoadBalancerAdminService
                .createOrUpdate(portLoadBalancerId, ports, mode);
        print("%s of %s executed", portLoadBalancer == null ? CREATE : UPDATE, portLoadBalancerId);

    }
}
