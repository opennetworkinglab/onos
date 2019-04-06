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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.workflow.api.ContextEventMapStore;
import org.onosproject.workflow.api.WorkflowException;

import java.util.Arrays;
import java.util.Objects;

@Service
@Command(scope = "onos", name = "workflow-eventmap", description = "workflow event map cli")
public class WorkFlowEventMapCommand extends AbstractShellCommand {

    static final String PRINT = "print";

    @Argument(index = 0, name = "cmd",
            description = "command(" + PRINT + ")", required = true)
    @Completion(WorkFlowEventMapCompleter.class)
    private String cmd = null;

    @Override
    protected void doExecute() {
        if (Objects.isNull(cmd)) {
            error("invalid cmd parameter");
            return;
        }

        ContextEventMapStore store = get(ContextEventMapStore.class);
        try {
            switch (cmd) {
                case PRINT:
                    JsonNode tree = store.asJsonTree();
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        print(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree));
                    } catch (JsonProcessingException e) {
                        error("Exception: " + e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
                    }
                    break;

                default:
                    print("Unsupported cmd: " + cmd);
            }
        } catch (WorkflowException e) {
            error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
        }
    }
}
