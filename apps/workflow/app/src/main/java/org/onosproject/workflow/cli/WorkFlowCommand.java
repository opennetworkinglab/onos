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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.workflow.api.DefaultWorkflowDescription;
import org.onosproject.workflow.api.WorkflowService;
import org.onosproject.workflow.api.WorkflowException;

import java.util.Arrays;
import java.util.Objects;

@Service
@Command(scope = "onos", name = "workflow", description = "workflow cli")
public class WorkFlowCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "cmd", description = "command(invoke)", required = true)
    private String cmd = null;

    @Argument (index = 1, name = "id", description = "workflow id(URI)", required = true)
    private String id = null;

    @Argument (index = 2, name = "name", description = "workplace name", required = true)
    private String name = null;

    @Override
    protected void doExecute() {
        if (Objects.isNull(cmd)) {
            error("invalid cmd parameter");
            return;
        }

        switch (cmd) {
            case "invoke":
                if (Objects.isNull(id)) {
                    error("invalid workflow id parameter");
                    return;
                }

                if (Objects.isNull(name)) {
                    error("invalid workplace name parameter");
                    return;
                }

                invoke(id, name);
                break;
            default:
                print("Unsupported cmd: " + cmd);
        }
    }

    /**
     * Invokes workflow.
     * @param workflowId workflow id
     * @param workplaceName workplace name
     */
    private void invoke(String workflowId, String workplaceName) {

        WorkflowService service = get(WorkflowService.class);
        DefaultWorkflowDescription wfDesc = DefaultWorkflowDescription.builder()
                .workplaceName(workplaceName)
                .id(workflowId)
                .data(JsonNodeFactory.instance.objectNode())
                .build();
        try {
            service.invokeWorkflow(wfDesc);
        } catch (WorkflowException e) {
            error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
        }
    }
}
