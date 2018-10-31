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
package org.onosproject.segmentrouting.cli;

import com.google.common.collect.Sets;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.PlaceholderCompleter;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.cli.net.PortNumberCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.xconnect.api.XconnectService;

import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Creates Xconnect.
 */
@Service
@Command(scope = "onos", name = "sr-xconnect-add", description = "Create Xconnect")
public class XconnectAddCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String deviceIdStr;

    @Argument(index = 1, name = "vlanId",
            description = "VLAN ID",
            required = true, multiValued = false)
    @Completion(PlaceholderCompleter.class)
    private String vlanIdStr;

    @Argument(index = 2, name = "port1",
            description = "Port 1. Can also specify L2 load balancer by L2LB(<key>)",
            required = true, multiValued = false)
    @Completion(PortNumberCompleter.class)
    private String port1Str;

    @Argument(index = 3, name = "port2",
            description = "Port 2. Can also specify L2 load balancer by L2LB(<key>)",
            required = true, multiValued = false)
    @Completion(PortNumberCompleter.class)
    private String port2Str;

    private static final String L2LB_PATTERN = "^(\\d*|L2LB\\(\\d*\\))$";

    @Override
    protected void doExecute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        VlanId vlanId = VlanId.vlanId(vlanIdStr);
        Set<String> ports = Sets.newHashSet(port1Str, port2Str);

        checkArgument(port1Str.matches(L2LB_PATTERN), "Wrong L2 load balancer format " + port1Str);
        checkArgument(port2Str.matches(L2LB_PATTERN), "Wrong L2 load balancer format " + port2Str);

        XconnectService xconnectService = get(XconnectService.class);
        xconnectService.addOrUpdateXconnect(deviceId, vlanId, ports);
    }
}
