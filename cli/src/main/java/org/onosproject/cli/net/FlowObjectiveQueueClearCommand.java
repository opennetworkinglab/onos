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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.flowobjective.FlowObjectiveService;

/**
 * Clear flow objective that are waiting for the completion of previous objective with the same key.
 */
@Service
@Command(scope = "onos", name = "obj-clear-queues",
        description = "Force empty flow objective queues and invalidate flow objective caches")
public class FlowObjectiveQueueClearCommand extends AbstractShellCommand {

    private static final String CONFIRM_PHRASE = "please";
    @Argument(name = "confirm", description = "Confirmation phrase")
    private String please = null;

    @Override
    protected void doExecute() {
        if (please == null || !please.equals(CONFIRM_PHRASE)) {
            print("WARNING: System may enter an unpredictable state if the flow obj queues are force emptied." +
                    "Enter confirmation phrase to continue.");
            return;
        }

        try {
            FlowObjectiveService service = get(FlowObjectiveService.class);
            service.clearQueue();
        } catch (ServiceNotFoundException e) {
            print("FlowObjectiveService unavailable");
        }
    }

}
