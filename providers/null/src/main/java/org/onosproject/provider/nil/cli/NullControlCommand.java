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
package org.onosproject.provider.nil.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.StartStopCompleter;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

import static org.onosproject.cli.StartStopCompleter.START;

/**
 * Starts or stops topology simulation.
 */
@Service
@Command(scope = "onos", name = "null-simulation",
        description = "Starts or stops topology simulation")
public class NullControlCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "cmd", description = "Control command: start/stop",
            required = true)
    @Completion(StartStopCompleter.class)
    String cmd = null;

    @Argument(index = 1, name = "topoShape",
            description = "Topology shape: e.g. configured, linear, reroute, " +
                    "centipede, tree, spineleaf, mesh, fattree, custom")
    @Completion(TopologyShapeCompleter.class)
    String topoShape = null;

    @Override
    protected void doExecute() {
        ComponentConfigService service = get(ComponentConfigService.class);
        // If there is an existing topology; make sure it's stopped before restarting
        if (cmd.equals(START)) {
            NullProviders npService = get(NullProviders.class);
            TopologySimulator simulator = npService.currentSimulator();
            if (simulator != null) {
                simulator.tearDownTopology();
            }
        }

        if (topoShape != null) {
            service.setProperty(NullProviders.class.getName(), "topoShape", topoShape);
        }
        service.setProperty(NullProviders.class.getName(), "enabled",
                            cmd.equals(START) ? "true" : "false");
    }

}
