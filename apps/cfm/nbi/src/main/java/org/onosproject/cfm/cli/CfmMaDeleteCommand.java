/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cfm.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cfm.cli.completer.CfmMaNameCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

/**
 * Delete a Maintenance Association from the existing list of a Maintenance Domain.
 */
@Service
@Command(scope = "onos", name = "cfm-ma-delete",
        description = "Delete a CFM Maintenance Association and its children.")
public class CfmMaDeleteCommand extends AbstractShellCommand {

    private static final int MA_NAME_PARTS_COUNT = 4;
    @Argument(name = "name",
            description = "Maintenance Domain name and type (in brackets) " +
                    "and the Maintenance Association name and type (in brackets)",
            required = true)
    @Completion(CfmMaNameCompleter.class)
    private String name = null;

    @Override
    protected void doExecute() {
        CfmMdService service = get(CfmMdService.class);

        String[] nameParts = name.split("[()]");
        if (nameParts.length != MA_NAME_PARTS_COUNT) {
            throw new IllegalArgumentException("Invalid name format. Must be in " +
                    "the format of <identifier(name-type)identifier(name-type)>");
        }

        MdId mdId = CfmMdListMdCommand.parseMdName(nameParts[0] + "(" + nameParts[1] + ")");

        MaIdShort maId = CfmMdListMdCommand.parseMaName(nameParts[2] + "(" + nameParts[3] + ")");

        try {
            boolean deleted = service.deleteMaintenanceAssociation(mdId, maId);
            print("Maintenance Association %s-%s is %ssuccessfully deleted.",
                    mdId, maId, deleted ? "" : "NOT ");
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
