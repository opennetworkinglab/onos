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
package org.onosproject.ofagent.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.ofagent.api.OFSwitch;
import org.onosproject.ofagent.api.OFSwitchService;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists virtual OF switches.
 */
@Service
@Command(scope = "onos", name = "ofagent-switches", description = "Lists all OF switches")
public class OFSwitchListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-35s%-8s";

    @Argument(index = 0, name = "network", description = "Virtual network ID",
            required = false, multiValued = false)
    private long networkId = NetworkId.NONE.id();

    @Override
    protected void doExecute() {
        OFSwitchService service = get(OFSwitchService.class);

        Set<OFSwitch> ofSwitches;
        if (networkId != NetworkId.NONE.id()) {
            ofSwitches = service.ofSwitches(NetworkId.networkId(networkId));
        } else {
            ofSwitches = service.ofSwitches();
        }

        print(FORMAT, "DPID", "Connected controllers");
        ofSwitches.forEach(ofSwitch -> {
            Set<String> channels = ofSwitch.controllerChannels().stream()
                    .map(channel -> channel.remoteAddress().toString())
                    .collect(Collectors.toSet());
            print(FORMAT, ofSwitch.dpid(), channels);
        });
    }
}
