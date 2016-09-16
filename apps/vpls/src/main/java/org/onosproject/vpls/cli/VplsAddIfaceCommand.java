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
import org.onosproject.vpls.config.VplsConfigurationService;

import java.util.Map;

/**
 * CLI to add an interface to a VPLS.
 */
@Command(scope = "onos", name = "vpls-add-iface",
        description = "Add an interface to an existing VPLS")
public class VplsAddIfaceCommand extends AbstractShellCommand {

    private static final String IFACE_ADD_FAIL = "Interface cannot be added.";
    private static final String IFACE_EXIST =
            "Interface %s already associated to network %s.";
    private VplsConfigurationService vplsConfigService =
            get(VplsConfigurationService.class);

    @Argument(index = 0, name = "vplsName", description = "Name of the VPLS",
            required = true, multiValued = false)
    private String vplsName = null;

    @Argument(index = 1, name = "ifaceName", description = "Name of the interface" +
            " to be added to the VPLS", required = true, multiValued = false)
    private String ifaceName = null;

    @Override
    protected void execute() {
        if (!vplsConfigService.getAllVpls().contains(vplsName)) {
            print(IFACE_ADD_FAIL);
            return;
        }

        if (vplsConfigService.getAllInterfaces()
                .stream()
                .anyMatch(e -> e.name().equals(ifaceName))) {
            print(IFACE_EXIST, ifaceName, vplsConfigService.getVplsNetworks()
                    .entries()
                    .stream()
                    .filter(e->e.getValue().name().equals(ifaceName))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .get());
            return;
        }

        vplsConfigService.addInterfaceToVpls(vplsName, ifaceName);
    }
}
