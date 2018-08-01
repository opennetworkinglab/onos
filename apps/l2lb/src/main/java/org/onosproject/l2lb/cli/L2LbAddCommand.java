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

import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.l2lb.api.L2LbAdminService;
import org.onosproject.l2lb.api.L2LbMode;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Command to add a L2 load balancer.
 */
@Service
@Command(scope = "onos", name = "l2lb-add", description = "Create or update L2 load balancer")
public class L2LbAddCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    private String deviceIdStr;

    @Argument(index = 1, name = "key",
            description = "L2 load balancer key",
            required = true, multiValued = false)
    private String keyStr;

    @Argument(index = 2, name = "mode",
            description = "L2 load balancer mode. STATIC or LACP",
            required = true, multiValued = false)
    private String modeStr;

    @Argument(index = 3, name = "ports",
            description = "L2 load balancer physical ports",
            required = true, multiValued = true)
    private String[] portsStr;

    @Override
    protected void doExecute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        int l2LbPort = Integer.parseInt(keyStr);

        L2LbMode mode = L2LbMode.valueOf(modeStr.toUpperCase());
        Set<PortNumber> ports = Sets.newHashSet(portsStr).stream()
                .map(PortNumber::fromString).collect(Collectors.toSet());

        L2LbAdminService l2LbAdminService = get(L2LbAdminService.class);
        l2LbAdminService.createOrUpdate(deviceId, l2LbPort, ports, mode);

    }
}
