/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.ofagent.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.ofagent.api.OFAgentService;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists the existing OFAgents.
 */
@Command(scope = "onos", name = "ofagents", description = "Lists all ofagents")
public class OFAgentListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-10s%-10s%-8s";
    private static final String CTRL = "%s:%s";

    @Override
    protected void execute() {
        OFAgentService service = get(OFAgentService.class);
        print(FORMAT, "Network", "Status", "Controllers");

        service.agents().forEach(agent -> {
            Set<String> ctrls = agent.controllers().stream()
                    .map(ctrl -> String.format(CTRL, ctrl.ip(), ctrl.port()))
                    .collect(Collectors.toSet());
            print(FORMAT, agent.networkId(), agent.state().name(), ctrls);
        });
    }
}
