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
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.WorkflowService;

import java.util.Arrays;
import java.util.Objects;

@Service
@Command(scope = "onos", name = "workflow-test", description = "workflow test cli")
public class WorkFlowTestCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "cmd", description = "command(invoke)", required = true)
    private String cmd = null;

    @Argument (index = 1, name = "number", description = "number of test", required = true)
    private String number = null;

    @Override
    protected void doExecute() {
        if (Objects.isNull(cmd)) {
            error("invalid cmd parameter");
            return;
        }

        switch (cmd) {
            case "invoke":
                if (Objects.isNull(number)) {
                    error("invalid number of test");
                    return;
                }

                int num;
                try {
                    num = Integer.parseInt(number);
                } catch (Exception e) {
                    error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
                    return;
                }

                test(num);
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
            error(e.getMessage() + "trace: " + Arrays.asList(e.getStackTrace()));
        }
    }

    /**
     * Workflow invoke test.
     * @param num the number of workflow to test
     */
    private void test(int num) {
        for (int i = 0; i <= num; i++) {
            String wpName = "test-" + i;
            invoke("sample.workflow-0", wpName);
        }
    }
}
