/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.cli;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.apache.karaf.shell.commands.Argument;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;
import org.onosproject.openstackvtap.api.OpenstackVtapId;

/**
 * Command line interface for removing openstack vTap rule.
 */
@Command(scope = "onos", name = "openstack-vtap-del",
        description = "OpenstackVtap deactivate")
public class OpenstackVtapDeleteCommand extends AbstractShellCommand {

    private final OpenstackVtapAdminService vTapService = get(OpenstackVtapAdminService.class);

    @Argument(index = 0, name = "id", description = "vTap ID",
            required = true, multiValued = false)
    String vTapId = "";

    @Override
    protected void execute() {
        OpenstackVtap vTap = vTapService.removeVtap(OpenstackVtapId.vTapId(vTapId));
        if (vTap != null) {
            print("Removed OpenstackVtap with id { %s }", vTap.id().toString());
        } else {
            print("Failed to remove OpenstackVtap with id { %s }", vTapId);
        }
    }
}
