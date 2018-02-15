/*
 * Copyright 2016-present Open Networking Foundation
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
import org.apache.karaf.shell.commands.Option;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

/**
 * Adds a simulated link to the custom topology simulation.
 */
@Command(scope = "onos", name = "null-create-link",
        description = "Adds a simulated link to the custom topology simulation")
public class CreateNullLink extends CreateNullEntity {

    @Argument(index = 0, name = "type", description = "Link type, e.g. direct, indirect, optical",
            required = true)
    String type = null;

    @Argument(index = 1, name = "src", description = "Source device name",
            required = true)
    String src = null;

    @Argument(index = 2, name = "dst", description = "Destination device name",
            required = true)
    String dst = null;

    @Option(name = "-u", aliases = "--unidirectional", description = "Unidirectional link only")
    private boolean unidirectional = false;

    @Override
    protected void execute() {
        NullProviders service = get(NullProviders.class);

        TopologySimulator simulator = service.currentSimulator();
        if (!(simulator instanceof CustomTopologySimulator)) {
            error("Custom topology simulator is not active.");
            return;
        }

        CustomTopologySimulator sim = (CustomTopologySimulator) simulator;
        ConnectPoint one = findAvailablePort(sim.deviceId(src), null);
        ConnectPoint two = findAvailablePort(sim.deviceId(dst), one);
        sim.createLink(one, two, Link.Type.valueOf(type.toUpperCase()), !unidirectional);
    }

}
