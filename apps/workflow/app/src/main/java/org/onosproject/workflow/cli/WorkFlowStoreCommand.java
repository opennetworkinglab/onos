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
package org.onosproject.workflow.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.workflow.api.Workflow;
import org.onosproject.workflow.api.WorkflowStore;

import java.util.Objects;
import java.net.URI;

@Service
@Command(scope = "onos", name = "workflowstore", description = "workflow store cli")
public class WorkFlowStoreCommand extends AbstractShellCommand {

    static final String RM = "rm";

    @Argument(index = 0, name = "cmd",
            description = "command(" + RM + ")", required = false)
    @Completion(WorkFlowStoreCompleter.class)
    private String cmd = null;

    @Argument(index = 1, name = "id",
            description = "workflow id(URI)", required = false)
    @Completion(WorkFlowIdCompleter.class)
    private String id = null;

    @Override
    protected void doExecute() {

        if (Objects.isNull(cmd)) {
            printAllWorkflow();
            return;
        }

        if (Objects.isNull(id)) {
            print("invalid id");
            return;
        }

        switch (cmd) {
            case RM:
                rmWorkflow(id);
                break;
            default:
                print("Unsupported cmd: " + cmd);
        }
    }

    private void rmWorkflow(String id) {
        WorkflowStore workflowStore = get(WorkflowStore.class);
        workflowStore.unregister(URI.create(id));
    }

    private void printAllWorkflow() {
        WorkflowStore workflowStore = get(WorkflowStore.class);
        for (Workflow workflow : workflowStore.getAll()) {
            print(getWorkflowString(workflow));
        }
    }

    private String getWorkflowString(Workflow workflow) {
        return workflow.toString();
    }
}
