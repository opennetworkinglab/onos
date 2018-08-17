/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.flowobjective.FlowObjectiveService;

/**
 * Manages FlowObjectiveComposition policy.
 */
@Service
@Command(scope = "onos", name = "policy",
        description = "Manages FlowObjectiveComposition policy")
public class FlowObjectiveCompositionCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "command",
            description = "Command name (install)",
            required = true, multiValued = false)
    String command = null;

    @Argument(index = 1, name = "names", description = "policy string",
            required = true, multiValued = true)
    String[] policies = null;

    @Override
    protected void doExecute() {
        FlowObjectiveService service = get(FlowObjectiveService.class);
        service.initPolicy(policies[0]);
        print("Policy %s installed", policies[0]);
    }
}
