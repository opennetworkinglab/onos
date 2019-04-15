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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.workflow.api.DefaultWorkflowDescription;
import org.onosproject.workflow.api.ImmutableListWorkflow;
import org.onosproject.workflow.api.Workflow;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.WorkflowService;
import org.onosproject.workflow.impl.example.SampleWorkflow;

import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

/**
 * Workflow test command.
 */
@Service
@Command(scope = "onos", name = "workflow-test", description = "workflow test cli")
public class WorkFlowTestCommand extends AbstractShellCommand {

    static final String INVOKE_SAMPLE = "invoke-sample";
    static final String INVOKE_INVALID_DATAMODEL_TYPE = "invoke-invalid-datamodel-type";
    static final String DEFINE_INVALID_WORKFLOW = "define-invalid-workflow";

    @Argument(index = 0, name = "test-name",
            description = "Test name (" + INVOKE_SAMPLE +
                    " | " + INVOKE_INVALID_DATAMODEL_TYPE + ")",
            required = true)
    @Completion(WorkFlowTestCompleter.class)
    private String testName = null;

    @Argument(index = 1, name = "arg1",
            description = "number of test for (" + INVOKE_SAMPLE +
                    " | " + INVOKE_INVALID_DATAMODEL_TYPE + ")",
            required = false)
    private String arg1 = null;

    @Override
    protected void doExecute() {
        if (Objects.isNull(testName)) {
            error("invalid test-name parameter");
            return;
        }

        switch (testName) {
            case INVOKE_SAMPLE:
                if (Objects.isNull(arg1)) {
                    error("arg1 is required for test " + INVOKE_SAMPLE);
                    return;
                }

                int num;
                try {
                    num = Integer.parseInt(arg1);
                } catch (NumberFormatException e) {
                    error("arg1 should be an integer value");
                    return;
                } catch (Exception e) {
                    error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
                    return;
                }

                invokeSampleTest(num);
                break;

            case INVOKE_INVALID_DATAMODEL_TYPE:
                if (Objects.isNull(arg1)) {
                    error("arg1 is required for test " + INVOKE_INVALID_DATAMODEL_TYPE);
                    return;
                }
                int count;
                try {
                    count = Integer.parseInt(arg1);
                } catch (NumberFormatException e) {
                    error("arg1 should be an integer value");
                    return;
                } catch (Exception e) {
                    error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
                    return;
                }

                invokeInvalidDatamodelTypeTest(count);
                break;

            case DEFINE_INVALID_WORKFLOW:
                defineInvalidWorkflow();
                break;

            default:
                print("Unsupported test-name: " + testName);
        }
    }

    /**
     * Workflow invoke test_name.
     *
     * @param num the arg1 of workflow to test_name
     */
    private void invokeSampleTest(int num) {
        for (int i = 0; i <= num; i++) {
            String wpName = "test_name-" + i;
            invoke("sample.workflow-0", wpName);
          //  invoke("sample.workflow-1", wpName);
          //  invoke("sample.workflow-2", wpName);
        }
    }

    /**
     * Workflow datatmodel type exception test.
     *
     * @param num the number of workflow to test
     */
    private void invokeInvalidDatamodelTypeTest(int num) {
        for (int i = 0; i <= num; i++) {
            String wpName = "test-" + i;
            invoke("sample.workflow-invalid-datamodel-type", wpName);
        }
    }


    /**
     * Invokes workflow.
     *
     * @param workflowId    workflow id
     * @param workplaceName workplace name
     */
    private void invoke(String workflowId, String workplaceName) {

        WorkflowService service = get(WorkflowService.class);

        ObjectNode dataModel = JsonNodeFactory.instance.objectNode();
        dataModel.put("count", 0);

        try {
            DefaultWorkflowDescription wfDesc = DefaultWorkflowDescription.builder()
                    .workplaceName(workplaceName)
                    .id(workflowId)
                    .data(dataModel)
                    .build();
            service.invokeWorkflow(wfDesc);
        } catch (WorkflowException e) {
            error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
        }
    }

    private void defineInvalidWorkflow() {

        WorkflowService service = get(WorkflowService.class);

        try {
            URI uri = URI.create("sample.workflow-invalid-datamodel-type");
            Workflow workflow = ImmutableListWorkflow.builder()
                    .id(uri)
                    .chain(SampleWorkflow.SampleWorklet5.class.getName())
                    .chain(SampleWorkflow.SampleWorklet6.class.getName())
                    .build();
            service.register(workflow);
        } catch (WorkflowException e) {
            error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
        }
    }

}
