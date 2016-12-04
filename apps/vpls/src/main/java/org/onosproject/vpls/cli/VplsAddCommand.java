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

import java.util.HashSet;

/**
 * CLI to create VPLSs.
 */
@Command(scope = "onos", name = "vpls-add", description = "Creates a new VPLS")
public class VplsAddCommand extends AbstractShellCommand {

    private VplsConfigService vplsConfigService =
            get(VplsConfigService.class);

    @Argument(index = 0, name = "vplsName", description = "Name of the VPLS",
            required = true, multiValued = false)
    private String vplsName = null;

    @Override
    protected void execute() {
        // Check if the VPLS name is already configured
        if (VplsCommandUtils.vplsExists(vplsName)) {
            print(VplsCommandUtils.VPLS_ALREADY_EXISTS, vplsName);
            return;
        }

        vplsConfigService.addVpls(vplsName, new HashSet<>(), null);
    }
}
