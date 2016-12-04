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
import org.onosproject.net.EncapsulationType;
import org.onosproject.vpls.config.VplsConfigService;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * CLI to show VPLS details.
 */
@Command(scope = "onos", name = "vpls-show",
        description = "Shows the details of an existing VPLS")
public class VplsShowCommand extends AbstractShellCommand {

    private VplsConfigService vplsConfigService =
            get(VplsConfigService.class);

    @Argument(index = 0, name = "vplsName", description = "Name of the VPLS",
            required = false, multiValued = false)
    private String vplsName = null;

    @Override
    protected void execute() {
        Set<String> vplsNames = vplsConfigService.vplsNames();
        Map<String, EncapsulationType> encapByVplsName =
                vplsConfigService.encapByVplsName();

        if (!isNullOrEmpty(vplsName)) {
            // A VPLS name is provided. Check first if the VPLS exists
            if (VplsCommandUtils.vplsExists(vplsName)) {
                print(VplsCommandUtils.VPLS_DISPLAY,
                      vplsName,
                      VplsCommandUtils.ifacesFromVplsName(vplsName).toString(),
                      encapByVplsName.get(vplsName).toString());
            } else {
                print(VplsCommandUtils.VPLS_NOT_FOUND, vplsName);
            }
        } else {
            // No VPLS names are provided. Display all VPLSs configured
            vplsNames.forEach(vplsName -> {
                print(VplsCommandUtils.VPLS_DISPLAY,
                      vplsName,
                      VplsCommandUtils.ifacesFromVplsName(vplsName).toString(),
                      encapByVplsName.get(vplsName).toString());
            });
        }
    }
}
