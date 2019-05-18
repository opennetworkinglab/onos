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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.workflow.api.WorkflowContext;
import org.onosproject.workflow.api.WorkflowException;
import org.onosproject.workflow.api.WorkflowService;
import org.onosproject.workflow.api.Workplace;
import org.onosproject.workflow.api.DefaultWorkplaceDescription;
import org.onosproject.workflow.api.WorkplaceStore;

import java.util.Arrays;
import java.util.Objects;

@Service
@Command(scope = "onos", name = "workplace",
        description = "workplace cli")
public class WorkplaceStoreCommand extends AbstractShellCommand {

    static final String ADD   = "add";
    static final String RM    = "rm";
    static final String CLEAR = "clear";
    static final String PRINT = "print";

    @Argument(index = 0, name = "cmd",
            description = "command(" + ADD + "/" + RM + "/" + CLEAR + "/" + PRINT + ")",
            required = false)
    @Completion(WorkplaceStoreCompleter.class)
    private String cmd = null;

    @Argument(index = 1, name = "name",
            description = "workplace name",
            required = false)
    @Completion(WorkplaceNameCompleter.class)
    private String name = null;

    @Option(name = "-wf", aliases = "--whitefilter", description = "whitelisting filter",
            required = false, multiValued = false)
    private String inFilter = null;

    @Option(name = "-bf", aliases = "--blackfilter", description = "blacklisting filter",
            required = false, multiValued = false)
    private String exFilter = null;

    @Option(name = "-nc", aliases = "--notcompleted", description = "not completed workflow context",
            required = false, multiValued = false)
    private boolean notCompleted = false;

    @Option(name = "-s", aliases = "--simpleprint", description = "simplified print format",
            required = false, multiValued = false)
    private boolean simplePrint = false;

    @Override
    protected void doExecute() {

        if (Objects.isNull(cmd)) {
            printAllWorkplace();
            return;
        }

        switch (cmd) {
            case ADD:
                if (Objects.isNull(name)) {
                    error("invalid name");
                    return;
                }
                addEmptyWorkplace(name);
                return;

            case RM:
                if (Objects.isNull(name)) {
                    print("invalid name");
                    return;
                }
                rmWorkplace(name);
                break;

            case CLEAR:
                clearWorkplace();
                break;

            case PRINT:
                if (Objects.isNull(name)) {
                    print("invalid name");
                    return;
                }
                printWorkplace(name);
                break;

            default:
                print("Unsupported cmd: " + cmd);
        }
    }

    /**
     * Adds empty workplace.
     * @param name workplace name
     */
    private void addEmptyWorkplace(String name) {
        WorkflowService service = get(WorkflowService.class);
        try {
            DefaultWorkplaceDescription wpDesc = DefaultWorkplaceDescription.builder()
                    .name(name)
                    .build();
            service.createWorkplace(wpDesc);
        } catch (WorkflowException e) {
            error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
        }
    }

    /**
     * Clears all workplaces.
     */
    private void clearWorkplace() {
        WorkflowService service = get(WorkflowService.class);
        try {
            service.clearWorkplace();
        } catch (WorkflowException e) {
            error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
        }
    }

    /**
     * Removes workplace.
     * @param name workplace name to remove
     */
    private void rmWorkplace(String name) {
        WorkflowService service = get(WorkflowService.class);
        try {
            DefaultWorkplaceDescription wpDesc = DefaultWorkplaceDescription.builder()
                    .name(name)
                    .build();
            service.removeWorkplace(wpDesc);
        } catch (WorkflowException e) {
            error(e.getMessage() + ", trace: " + Arrays.asList(e.getStackTrace()));
        }
    }

    /**
     * Prints workplace.
     * @param name workplace name
     */
    private void printWorkplace(String name) {
        WorkplaceStore workplaceStore = get(WorkplaceStore.class);
        Workplace workplace = workplaceStore.getWorkplace(name);
        if (Objects.isNull(workplace)) {
            print("Not existing workplace " + name);
            return;
        }
        print(getWorkplaceString(workplace));
        printWorkplaceContexts(workplaceStore, workplace.name());
    }

    /**
     * Prints all workplaces.
     */
    private void printAllWorkplace() {
        WorkplaceStore workplaceStore = get(WorkplaceStore.class);
        for (Workplace workplace : workplaceStore.getWorkplaces()) {
            print(getWorkplaceString(workplace));
            printWorkplaceContexts(workplaceStore, workplace.name());
        }
    }

    /**
     * Prints contexts of workplace.
     * @param workplaceStore workplace store service
     * @param workplaceName workplace name
     */
    private void printWorkplaceContexts(WorkplaceStore workplaceStore, String workplaceName) {
        for (WorkflowContext context : workplaceStore.getWorkplaceContexts(workplaceName)) {

            if (notCompleted && context.current().isCompleted()) {
                continue;
            }

            String str = context.toString();
            if (Objects.nonNull(inFilter)) {
                if (str.indexOf(inFilter) != -1) {
                    if (Objects.nonNull(exFilter)) {
                        if (str.indexOf(exFilter) == -1) {
                            printContext(context);
                        }
                    } else {
                        printContext(context);
                    }
                }
            } else {
                if (Objects.nonNull(exFilter)) {
                    if (str.indexOf(exFilter) == -1) {
                        printContext(context);
                    }
                } else {
                    printContext(context);
                }
            }
        }
    }

    /**
     * Prints a workflow context.
     * @param context a workflow context
     */
    private void printContext(WorkflowContext context) {
        if (simplePrint) {
            String str = " - "
                     + "name=" + context.name()
                     + ", state=" + context.state()
                     + ", current=" + context.current().workletType();
            print(str);

        } else {
            print(" - " + context);
        }
    }

    /**
     * Gets workplace string.
     * @param workplace workplace
     * @return workplace string
     */
    private String getWorkplaceString(Workplace workplace) {
        if (simplePrint) {
            return workplace.name();
        } else {
            return workplace.toString();
        }
    }
}
