/*
 * Copyright 2014 Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.edge.EdgePortService;

import static org.onosproject.net.DeviceId.deviceId;

/**
 * Lists all edge ports.
 */
@Command(scope = "onos", name = "edge-ports",
        description = "Lists all edge ports.")
public class EdgePortsListCommand extends AbstractShellCommand {

    private static final String FMT = "%s/%s";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    String uri = null;



    @Override
    protected void execute() {
        EdgePortService service = get(EdgePortService.class);
        if (uri == null) {
            service.getEdgePoints().forEach(e -> print(FMT, e.deviceId(), e.port()));
        } else {
            service.getEdgePoints(deviceId(uri)).forEach(e -> print(FMT, e.deviceId(), e.port()));
        }
    }

}
