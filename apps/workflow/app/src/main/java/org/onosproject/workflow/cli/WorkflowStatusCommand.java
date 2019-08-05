/*
 * Copyright 2019-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.workflow.api.WorkplaceStore;
import org.onosproject.workflow.api.WorkflowStore;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.Workflow;
import org.onosproject.workflow.api.JsonDataModelTree;
import org.onosproject.workflow.api.WorkflowException;


import java.util.Objects;

@Service
@Command(scope = "onos", name = "workflow-nodes", description = "workflow status cli")
public class WorkflowStatusCommand extends AbstractShellCommand {

    static final String STATUS = "status";

    @Argument(index = 0, name = "cmd",
            description = "command(" + STATUS + "eval)",
            required = true)
    private String cmd = null;

    @Override
    protected void doExecute() {
        if (Objects.isNull(cmd)) {
            error("invalid cmd parameter");
            return;
        }


        switch (cmd) {
            case STATUS:
                invoke();
                break;
            default:
                print("Unsupported cmd: " + cmd);
        }
    }


    private void invoke() {
        try {
            WorkflowStore workflowStore = get(WorkflowStore.class);
            WorkplaceStore workplaceStore = get(WorkplaceStore.class);
            System.out.printf("%-25s %-45s %-10s%n", "DEVICEIP", " WORKFLOW NAME", "WORKFLOW STATE");
            for (WorkflowContext context : workplaceStore.getContexts()) {
                for (Workflow workflow : workflowStore.getAll()) {
                    if (context.workflowId().equals(workflow.id())) {
                        JsonDataModelTree tree = (JsonDataModelTree) context.data();
                        JsonNode mgmtIp = tree.nodeAt("/mgmtIp");
                        System.out.printf("%-25s %-45s %-10s%n", mgmtIp, context.name(), context.state().toString());
                    }

                }
            }

        } catch (WorkflowException e) {
            e.printStackTrace();
        }

    }
}

