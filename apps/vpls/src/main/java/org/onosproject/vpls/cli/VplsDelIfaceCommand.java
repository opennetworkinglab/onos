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

package org.onosproject.vpls.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.vpls.config.VplsConfigurationService;

import java.util.Set;

/**
 * CLI to remove an interface from an existing VPLS.
 */
@Command(scope = "onos", name = "vpls-del-iface",
        description = "Removes an interface from an existing VPLS")
public class VplsDelIfaceCommand extends AbstractShellCommand {

    private static final String NO_CONFIGURATION = "Interface %s is not configured";
    private VplsConfigurationService vplsConfigService =
            get(VplsConfigurationService.class);

    @Argument(index = 0, name = "IFACE_NAME", description = "Name of the interface" +
            " to remove from the VPLS", required = true, multiValued = false)
    private String ifaceName = null;

    @Override
    protected void execute() {
        Set<Interface> ifaces = vplsConfigService.getAllInterfaces();

        if (!ifaces.stream().map(Interface::name).anyMatch(ifaceName::equals)) {
            print(NO_CONFIGURATION, ifaceName);
        }

        vplsConfigService.removeInterfaceFromVpls(ifaceName);
    }

}
