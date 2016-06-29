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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.provider.nil.NullProviders;

import static org.onosproject.cli.UpDownCompleter.DOWN;
import static org.onosproject.cli.UpDownCompleter.UP;

/**
 * Severs or repairs a simulated link.
 */
@Command(scope = "onos", name = "null-link",
        description = "Severs or repairs a simulated link")
public class NullLinkCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "one", description = "One link end-point as device/port",
            required = true, multiValued = false)
    String one = null;

    @Argument(index = 1, name = "two", description = "Another link end-point as device/port",
            required = true, multiValued = false)
    String two = null;

    @Argument(index = 2, name = "cmd", description = "up/down",
            required = true, multiValued = false)
    String cmd = null;


    @Override
    protected void execute() {
        NullProviders service = get(NullProviders.class);

        try {
            ConnectPoint onePoint = ConnectPoint.deviceConnectPoint(one);
            ConnectPoint twoPoint = ConnectPoint.deviceConnectPoint(two);

            if (cmd.equals(UP)) {
                service.repairLink(onePoint, twoPoint);
            } else if (cmd.equals(DOWN)) {
                service.severLink(onePoint, twoPoint);
            } else {
                error("Illegal command %s; must be up or down", cmd);
            }
        } catch (NumberFormatException e) {
            error("Invalid port number specified", e);
        }
    }

}
