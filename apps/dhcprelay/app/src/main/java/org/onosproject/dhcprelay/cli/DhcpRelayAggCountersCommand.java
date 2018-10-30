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

package org.onosproject.dhcprelay.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.dhcprelay.api.DhcpRelayService;
import org.onosproject.dhcprelay.store.DhcpRelayCounters;
import org.onosproject.dhcprelay.store.DhcpRelayCountersStore;


import java.util.Map;
import java.util.Optional;

/**
 * Prints Dhcp FPM Routes information.
 */
@Service
@Command(scope = "onos", name = "dhcp-relay-agg-counters",
         description = "DHCP Relay Aggregate Counters cli.")
public class DhcpRelayAggCountersCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "reset",
            description = "reset counters or not",
            required = false, multiValued = false)
    @Completion(DhcpRelayResetCompleter.class)
    String reset = null;

    private static final String HEADER = "DHCP Relay Aggregate Counters :";
    private static final String GCOUNT = "global";
    private static final DhcpRelayService DHCP_RELAY_SERVICE = get(DhcpRelayService.class);

    @Override
    protected void doExecute() {
        boolean toResetFlag;

        if (reset != null) {
            if (reset.equals("reset") || reset.equals("[reset]")) {
                toResetFlag = true;
            } else {
                print("Last parameter is [reset]");
                return;
            }
        } else {
            toResetFlag = false;
        }

        print(HEADER);

        DhcpRelayCountersStore counterStore = AbstractShellCommand.get(DhcpRelayCountersStore.class);

        Optional<DhcpRelayCounters> perClassCounters = counterStore.getCounters(GCOUNT);

        if (perClassCounters.isPresent()) {
            Map<String, Integer> counters = perClassCounters.get().getCounters();
            if (counters.size() > 0) {
                counters.forEach((name, value) -> {
                    print("%-30s  ............................  %-4d packets", name, value);
                });
            } else {
                print("No counter for {}", GCOUNT);
            }

            if (toResetFlag) {
                counterStore.resetCounters(GCOUNT);

            }
        }
    }
}
