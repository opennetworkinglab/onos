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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cfm.cli.completer.CfmMdLevelCompleter;
import org.onosproject.cfm.cli.completer.CfmMdNameTypeCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.PlaceholderCompleter;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

/**
 * Adds a Maintenance Domain to the existing list.
 */
@Service
@Command(scope = "onos", name = "cfm-md-add",
        description = "Add a CFM Maintenance Domain.")
public class CfmMdAddCommand extends AbstractShellCommand {

    @Argument(name = "name-type",
            description = "Maintenance Domain name type",
            required = true)
    @Completion(CfmMdNameTypeCompleter.class)
    private String nameType = null;

    @Argument(index = 1, name = "name",
            description = "Maintenance Domain name. Restrictions apply depending " +
                    "on name-type. Leave empty if name type is none",
            required = true)
    @Completion(PlaceholderCompleter.class)
    private String name = null;

    @Argument(index = 2, name = "level",
            description = "Maintenance Domain level LEVEL0-LEVEL7",
            required = true)
    @Completion(CfmMdLevelCompleter.class)
    private String level = null;

    @Argument(index = 3, name = "numeric-id",
            description = "An optional numeric id for Maintenance Domain [1-65535]")
    @Completion(PlaceholderCompleter.class)
    private Short numericId = null;

    @Override
    protected void doExecute() {
        CfmMdService service = get(CfmMdService.class);
        MdId mdId = CfmMdListMdCommand.parseMdName(name + "(" + nameType + ")");

        MaintenanceDomain.MdLevel levelEnum =
                MaintenanceDomain.MdLevel.valueOf(level);

        try {
            MaintenanceDomain.MdBuilder builder = DefaultMaintenanceDomain
                    .builder(mdId).mdLevel(levelEnum);
            if (numericId != null) {
                builder = builder.mdNumericId(numericId);
            }
            boolean created = service.createMaintenanceDomain(builder.build());
            print("Maintenance Domain with id %s is successfully %s.",
                    mdId, created ? "updated" : "created");
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
