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

/**
 * This class defines the cli command for the PatchPanel class. It creates
 * an instance of the PatchPanelService class to call it's method addPatch().
 * The command takes 2 parameters, 2 connectPoints.
 */
package org.onosproject.patchpanel.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.patchpanel.impl.Patch;
import org.onosproject.patchpanel.impl.PatchPanelService;

/**
 * Lists the patches.
 */
@Command(scope = "onos", name = "patches",
         description = "Lists the patches")
public class PatchListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%s: %s <-> %s";

    @Override
    protected void execute() {
        PatchPanelService patchPanelService = get(PatchPanelService.class);

        patchPanelService.getPatches().forEach(this::print);
    }

    private void print(Patch patch) {
        print(FORMAT, patch.id(), patch.port1(), patch.port2());
    }

}
