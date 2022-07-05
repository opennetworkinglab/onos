/*
 * Copyright 2022-present Open Networking Foundation
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
import org.onosproject.workflow.api.WorkflowLogStore;

import java.util.List;
import java.util.Map;

/**
 * Displays workflow logs of each context.
 */
@Service
@Command(scope = "onos", name = "workflow-logs", description = "Workflow Logs CLI")
public class WorkflowLogStoreCommand extends AbstractShellCommand {

    protected static final String SHOW = "show";

     // Executes this command.
    @Argument(name = "command", description = "command (" + SHOW + ")", required = true)
    @Completion(WorkflowLogStoreCompleter.class)
    private String command;

    @Argument(index = 1, name = "arg1")
    @Completion(WorkflowLogStoreCtxtNameCompleter.class)
    private String arg1;

    @Override
    protected void doExecute() {
        switch (command) {
            case SHOW:
                showLogs();
                break;
            default:
                error("Wrong command - " + command);
                break;
        }
    }

    private void showLogs() {
        WorkflowLogStore workflowLogStore = get(WorkflowLogStore.class);
        Map<String, List<String>> logMap = workflowLogStore.asJavaMap();

        if (arg1 == null) {
            logMap.forEach(this::printLogs);
            return;
        }

        if (!logMap.containsKey(arg1)) {
            error("There are no logs for key:" + arg1);
            return;
        }
        printLogs(arg1, logMap.get(arg1));
    }

    private void printLogs(String contextName, List<String> logs) {
        String msg = "Context Name: " + contextName + "\n"
                + "Log:\n" + String.join("\n", logs) + "\n";
        print(msg);
    }
}
