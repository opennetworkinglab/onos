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
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.workflow.api.DefaultWorkflowDescription;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.WorkflowExecutionService;
import org.onosproject.workflow.api.WorkflowService;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.WorkplaceStore;

import java.util.Objects;

@Service
@Command(scope = "onos", name = "workflow", description = "workflow cli")
public class WorkFlowCommand extends AbstractShellCommand {

    static final String INVOKE = "invoke";
    static final String EVAL   = "eval";



    @Argument(index = 0, name = "cmd",
            description = "command(" + INVOKE + "|" + EVAL + "eval)",
            required = true)
    @Completion(WorkFlowCompleter.class)
    private String cmd = null;

    @Argument(index = 1, name = "name",
            description = "workflow context name(workflow@workplace)",
            required = true)
    @Completion(WorkFlowCtxtNameCompleter.class)
    private String name = null;

    @Override
    protected void doExecute() {
        if (Objects.isNull(cmd)) {
            error("invalid cmd parameter");
            return;
        }

        if (Objects.isNull(name)) {
            error("invalid workflow context name");
            return;
        }

        String[] tokens = name.split("@");
        if (tokens != null && tokens.length != 2) {
            error("invalid workflow context name(workflow@workplace)");
            return;
        }

        String workflowId = tokens[0];
        String workplace = tokens[1];

        switch (cmd) {
            case INVOKE:
                invoke(workflowId, workplace);
                break;
            case EVAL:
                eval(name);
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
        try {
            DefaultWorkflowDescription wfDesc = DefaultWorkflowDescription.builder()
                    .workplaceName(workplaceName)
                    .id(workflowId)
                    .data(JsonNodeFactory.instance.objectNode())
                    .build();

            service.invokeWorkflow(wfDesc);
        } catch (WorkflowException e) {
            error("Exception: ", e);
        }
    }

    /**
     * Evaluates workflow context.
     * @param workflowContextName workflow context name
     */
    private void eval(String workflowContextName) {
        WorkplaceStore storService = get(WorkplaceStore.class);
        WorkflowExecutionService execService = get(WorkflowExecutionService.class);

        WorkflowContext context = storService.getContext(workflowContextName);
        if (context == null) {
            error("failed to find workflow context {}", workflowContextName);
            return;
        }
        execService.eval(workflowContextName);
    }

}
