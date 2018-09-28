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
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.segmentrouting.SegmentRoutingService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

/**
 * Command to read the current state of the pseudowire next stores.
 */
@Service
@Command(scope = "onos", name = "sr-next-pw",
        description = "Displays the current next-id for pseudowire")
public class PseudowireNextListCommand extends AbstractShellCommand {
    @Override
    protected void doExecute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);
        print(srService.getPwInitNext());
        print(srService.getPwTermNext());
    }

    private void print(Map<String, NextObjective> nextStore) {
        ArrayList<String> a = new ArrayList<>(nextStore.keySet());
        a.sort(Comparator.comparing((String o) -> o));

        StringBuilder builder = new StringBuilder();
        a.forEach(k ->
            builder.append("\n")
                    .append(k)
                    .append(" --> ")
                    .append(nextStore.get(k).id())
        );
        print(builder.toString());
    }
}
