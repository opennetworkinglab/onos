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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.storekey.MacVlanNextObjectiveStoreKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

/**
 * Command to read the current state of the macVlanNextObjStore.
 */
@Service
@Command(scope = "onos", name = "sr-next-mac-vlan",
        description = "Displays the current next-hop / next-id it mapping")
public class NextMacVlanCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        print(srService.getMacVlanNextObjStore());
    }

    private void print(Map<MacVlanNextObjectiveStoreKey, Integer> macVlanNextObjStore) {
        ArrayList<MacVlanNextObjectiveStoreKey> a = new ArrayList<>(macVlanNextObjStore.keySet());
        a.sort(Comparator
                .comparing((MacVlanNextObjectiveStoreKey o) -> o.deviceId().toString())
                .thenComparing((MacVlanNextObjectiveStoreKey o) -> o.vlanId().toString())
                .thenComparing((MacVlanNextObjectiveStoreKey o) -> o.macAddr().toString()));

        StringBuilder builder = new StringBuilder();
        a.forEach(k ->
            builder.append("\n")
                    .append(k)
                    .append(" --> ")
                    .append(macVlanNextObjStore.get(k))
        );
        print(builder.toString());
    }
}
