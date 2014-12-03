/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.store.service.DatabaseAdminService;

import java.util.Optional;

/**
 * Lists mastership roles of nodes for each device.
 */
@Command(scope = "onos", name = "tablet-leader",
         description = "Prints the current leader of a tablet.")
public class TabletLeaderCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        final DatabaseAdminService dbAdminService = get(DatabaseAdminService.class);

        Optional<ControllerNode> leader = dbAdminService.leader();
        if (leader.isPresent()) {
            print("Leader: %s", leader.get());
        } else {
            print("No Leader");
        }
    }
}
