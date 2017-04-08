/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

import static org.onosproject.cli.StartStopCompleter.START;

/**
 * Starts or stops topology simulation.
 */
@Command(scope = "onos", name = "null-simulation",
        description = "Starts or stops topology simulation")
public class NullControlCommand extends AbstractShellCommand {

    private static final String CUSTOM = "custom";

    @Argument(index = 0, name = "cmd", description = "Control command: start/stop",
            required = true, multiValued = false)
    String cmd = null;

    @Argument(index = 1, name = "topoShape",
            description = "Topology shape: e.g. configured, linear, reroute, " +
                    "centipede, tree, spineleaf, mesh, fattree, custom",
            required = false, multiValued = false)
    String topoShape = null;

    @Override
    protected void execute() {
        ComponentConfigService service = get(ComponentConfigService.class);
        if (topoShape != null) {
            service.setProperty(NullProviders.class.getName(), "topoShape", topoShape);
        }
        service.setProperty(NullProviders.class.getName(), "enabled",
                            cmd.equals(START) ? "true" : "false");

        // If we are re-starting the "custom" topology, reset the counts
        //  on the auto-assigned IDs for null-devices and null-hosts, so that
        //  scripts can rely on consistent assignment of IDs to nodes.
        if (CUSTOM.equals(topoShape) && START.equals(cmd)) {
            NullProviders npService = get(NullProviders.class);
            TopologySimulator simulator = npService.currentSimulator();
            if (simulator instanceof CustomTopologySimulator) {
                CustomTopologySimulator sim = (CustomTopologySimulator) simulator;
                sim.resetIdSeeds();
            }
        }
    }

}
