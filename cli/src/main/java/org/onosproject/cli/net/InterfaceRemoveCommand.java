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

package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.intf.InterfaceAdminService;
import org.onosproject.net.ConnectPoint;

/**
 * Removes an interface configuration.
 */
@Command(scope = "onos", name = "interface-remove",
        description = "Removes a configured interface")
public class InterfaceRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "connectPoint",
            description = "Connect point of the interface",
            required = true, multiValued = false)
    private String connectPoint = null;

    @Argument(index = 1, name = "name",
            description = "Interface name",
            required = true, multiValued = false)
    private String name = null;

    @Override
    protected void execute() {
        InterfaceAdminService interfaceService = get(InterfaceAdminService.class);

        boolean success = interfaceService.remove(
                ConnectPoint.deviceConnectPoint(connectPoint), name);

        if (success) {
            print("Interface removed");
        } else {
            print("Unable to remove interface");
        }
    }

}
