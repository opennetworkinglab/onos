/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.host.HostService;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.NullProviders;
import org.onosproject.provider.nil.TopologySimulator;

import java.util.Iterator;
import java.util.Objects;

/**
 * Adds a simulated link to the custom topology simulation.
 */
@Command(scope = "onos", name = "null-create-link",
        description = "Adds a simulated link to the custom topology simulation")
public class CreateNullLink extends AbstractShellCommand {

    @Argument(index = 0, name = "type", description = "Link type, e.g. direct, indirect, optical",
            required = true, multiValued = false)
    String type = null;

    @Argument(index = 1, name = "src", description = "Source device name",
            required = true, multiValued = false)
    String src = null;

    @Argument(index = 2, name = "dst", description = "Destination device name",
            required = true, multiValued = false)
    String dst = null;

    @Option(name = "-u", aliases = "--unidirectional", description = "Unidirectional link only",
            required = false, multiValued = false)
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

    // Finds an available connect point among edge ports of the specified device
    private ConnectPoint findAvailablePort(DeviceId deviceId, ConnectPoint otherPoint) {
        EdgePortService eps = get(EdgePortService.class);
        HostService hs = get(HostService.class);
        Iterator<ConnectPoint> points = eps.getEdgePoints(deviceId).iterator();

        while (points.hasNext()) {
            ConnectPoint point = points.next();
            if (!Objects.equals(point, otherPoint) && hs.getConnectedHosts(point).isEmpty()) {
                return point;
            }
        }
        return null;
    }

}
