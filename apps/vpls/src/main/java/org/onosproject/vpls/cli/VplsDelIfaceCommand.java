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
import org.onosproject.vpls.config.VplsConfigService;

/**
 * CLI to remove an interface from an existing VPLS.
 */
@Command(scope = "onos", name = "vpls-del-iface",
        description = "Removes an interface from an existing VPLS")
public class VplsDelIfaceCommand extends AbstractShellCommand {

    private VplsConfigService vplsConfigService =
            get(VplsConfigService.class);

    @Argument(index = 0, name = "vplsName", description = "Name of the VPLS",
            required = true, multiValued = false)
    private String vplsName = null;

    @Argument(index = 1, name = "ifaceName", description = "Name of the interface" +
            " to be removed from the VPLS", required = true, multiValued = false)
    private String ifaceName = null;

    @Override
    protected void execute() {
        if (!VplsCommandUtils.vplsExists(vplsName)) {
            print(VplsCommandUtils.VPLS_NOT_FOUND, vplsName);
            return;
        }

        if (!VplsCommandUtils.ifaceExists(ifaceName)) {
            print(VplsCommandUtils.IFACE_NOT_FOUND, ifaceName);
            return;
        }

        if (!VplsCommandUtils.ifaceAlreadyAssociated(ifaceName)) {
            print(VplsCommandUtils.IFACE_NOT_ASSOCIATED,
                  ifaceName, VplsCommandUtils.vplsNameFromIfaceName(ifaceName));
            return;
        }

        vplsConfigService.removeIface(ifaceName);
    }

}
