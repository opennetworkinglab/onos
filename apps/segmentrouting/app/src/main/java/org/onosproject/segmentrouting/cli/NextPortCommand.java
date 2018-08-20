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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.storekey.PortNextObjectiveStoreKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

/**
 * Command to read the current state of the portNextObjStore.
 */
@Command(scope = "onos", name = "sr-next-port",
        description = "Displays the current port / next-id it mapping")
public class NextPortCommand extends AbstractShellCommand {
    @Override
    protected void execute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        print(srService.getPortNextObjStore());
    }

    private void print(Map<PortNextObjectiveStoreKey, Integer> portNextObjStore) {
        ArrayList<PortNextObjectiveStoreKey> a = new ArrayList<>(portNextObjStore.keySet());
        a.sort(Comparator
                .comparing((PortNextObjectiveStoreKey o) -> o.deviceId().toString())
                .thenComparing((PortNextObjectiveStoreKey o) -> o.portNumber().toLong()));

        StringBuilder builder = new StringBuilder();
        a.forEach(k ->
            builder.append("\n")
                    .append(k)
                    .append(" --> ")
                    .append(portNextObjStore.get(k))
        );
        print(builder.toString());
    }
}
