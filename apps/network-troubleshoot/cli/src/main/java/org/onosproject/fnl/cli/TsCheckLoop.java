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
package org.onosproject.fnl.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.fnl.intf.NetworkDiagnostic;
import org.onosproject.fnl.intf.NetworkDiagnosticService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;

/**
 * Search for all potential routing loops.
 */
@Command(scope = "onos",
        name = "ts-check-loops",
        description = "Check if there are some routing loops in the network",
        detailedDescription = "Report the header of loop-trigger packet, " +
                "DevicesIds and FlowEntries.")
public class TsCheckLoop extends AbstractShellCommand {

    @Override
    protected void execute() {
        NetworkDiagnosticService service = getService(NetworkDiagnosticService.class);

        DeviceService ds = getService(DeviceService.class);
        HostService hs = getService(HostService.class);
        FlowRuleService frs = getService(FlowRuleService.class);
        LinkService ls = getService(LinkService.class);

        service.findAnomalies(NetworkDiagnostic.Type.LOOP)
                .forEach(loop -> print(loop.toString()));
    }
}
