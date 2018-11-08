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
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;
import org.onosproject.openstackvtap.api.OpenstackVtapId;

/**
 * Delete a openstack vtap rule from the existing vtaps.
 */
@Service
@Command(scope = "onos", name = "openstack-vtap-del",
        description = "OpenstackVtap deactivate")
public class OpenstackVtapDeleteCommand extends AbstractShellCommand {

    private final OpenstackVtapAdminService vtapService = get(OpenstackVtapAdminService.class);

    @Argument(index = 0, name = "id", description = "vtap ID",
            required = true, multiValued = false)
    @Completion(VtapIdCompleter.class)
    String vtapId = "";

    @Override
    protected void doExecute() {
        OpenstackVtap vtap = vtapService.removeVtap(OpenstackVtapId.vtapId(vtapId));
        if (vtap != null) {
            print("Removed OpenstackVtap with id { %s }", vtap.id().toString());
        } else {
            print("Failed to remove OpenstackVtap with id { %s }", vtapId);
        }
    }
}
