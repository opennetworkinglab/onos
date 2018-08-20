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


import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingService;

/**
 * Command to invalidate next id from SR internal stores.
 */
@Command(scope = "onos", name = "sr-next-invalidate",
        description = "Invalidate given next id from SR internal stores")
public class InvalidateNextCommand extends AbstractShellCommand {

    private static final String CONFIRM_PHRASE = "please";

    @Argument(name = "nextId", description = "Next ID", index = 0)
    private String nextId = null;

    @Argument(name = "confirm", description = "Confirmation phrase", index = 1)
    private String please = null;

    @Override
    protected void execute() {
        if (please == null || !please.equals(CONFIRM_PHRASE)) {
            print("WARNING: System may enter an unpredictable state if the next ID is force invalidated." +
                    "Enter confirmation phrase to continue.");
            return;
        }

        try {
            SegmentRoutingService srService = AbstractShellCommand.get(SegmentRoutingService.class);
            srService.invalidateNextObj(Integer.parseInt(nextId));
        } catch (ServiceNotFoundException e) {
            print("SegmentRoutingService unavailable");
        }
    }
}
