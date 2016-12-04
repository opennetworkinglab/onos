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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vpls.config.VplsConfigService;

/**
 * CLI to list VPLSs.
 */
@Command(scope = "onos", name = "vpls-list", description = "List the VPLSs configured")
public class VplsListCommand extends AbstractShellCommand {
    private VplsConfigService vplsConfigService =
            get(VplsConfigService.class);

    @Override
    protected void execute() {
        vplsConfigService.vplsNames().forEach(vpls -> {
            print(VplsCommandUtils.VPLS_NAME, vpls);
        });
    }
}
