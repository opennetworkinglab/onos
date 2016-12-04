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
 * CLI to set encapsulation for a VPLS.
 */
@Command(scope = "onos", name = "vpls-set-encap",
        description = "Sets the encapsulation type for a given VPLS. None means" +
                "no encapsulation has been set")
public class VplsSetEncapCommand extends AbstractShellCommand {

    private static final String VPLS_NOT_FOUND = "VPLS %s not found.";
    private VplsConfigService vplsConfigService =
            get(VplsConfigService.class);

    @Argument(index = 0, name = "vplsName", description = "Name of the VPLS",
            required = true, multiValued = false)
    private String vplsName = null;

    @Argument(index = 1, name = "encapsulation", description = "The encapsulation" +
            "type. For example, VLAN or MPLS. None for no encapsulation",
            required = true, multiValued = false)
    String encap = null;

    @Override
    protected void execute() {
        if (!VplsCommandUtils.vplsExists(vplsName)) {
            print(VplsCommandUtils.VPLS_NOT_FOUND, vplsName);
            return;
        }

        vplsConfigService.setEncap(vplsName, encap);
    }
}
