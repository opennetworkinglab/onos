/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cli.net;

import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.flowobjective.FlowObjectiveService;

/**
 * Returns a list of FlowObjective next-ids waiting to get created by device-drivers.
 * Also returns the forwarding objectives and next objectives waiting on the pending
 * next-objectives. These lists are controller instance specific.
 */
@Service
@Command(scope = "onos", name = "obj-pending-nexts",
        description = "flow-objectives pending next-objectives")
public class FlowObjectivePendingNextCommand extends AbstractShellCommand {

    private static final String FORMAT_MAPPING = "  %s";

    @Override
    protected void doExecute() {
        try {
            FlowObjectiveService service = get(FlowObjectiveService.class);
            printNexts(service.getPendingFlowObjectives());
        } catch (ServiceNotFoundException e) {
            print(FORMAT_MAPPING, "FlowObjectiveService unavailable");
        }
    }

    private void printNexts(List<String> pendingNexts) {
        pendingNexts.forEach(str -> print(FORMAT_MAPPING, str));
    }

}
