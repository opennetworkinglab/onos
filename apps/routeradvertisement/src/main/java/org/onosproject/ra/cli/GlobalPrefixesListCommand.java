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

package org.onosproject.ra.cli;

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.ra.RoutingAdvertisementService;
import org.onosproject.cli.AbstractShellCommand;

import org.apache.karaf.shell.api.action.Command;

import com.google.common.collect.ImmutableMap;

/**
 * Command to list global-prefixes in Routing Advertisement.
 */
@Service
@Command(scope = "onos", name = "ra-global-prefixes",
        description = "List Routing Advertisement global prefixes")
public class GlobalPrefixesListCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        RoutingAdvertisementService raService =
                AbstractShellCommand.get(RoutingAdvertisementService.class);
        printGlobalPrefixes(raService.getGlobalPrefixes());
    }

    private void printGlobalPrefixes(ImmutableMap<DeviceId, List<InterfaceIpAddress>> globalPrefixes) {
        globalPrefixes.forEach(((deviceId, interfaceIpAddresses) -> {
            print("%s", deviceId);
            interfaceIpAddresses.forEach(interfaceIpAddress -> print("    %s", interfaceIpAddress));
        }));
    }
}

