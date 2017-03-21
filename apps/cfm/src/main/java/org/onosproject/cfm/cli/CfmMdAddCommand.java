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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Argument;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

import java.util.Optional;

/**
 * Adds a Maintenance Domain to the existing list.
 */
@Command(scope = "onos", name = "cfm-md-add",
        description = "Add a CFM Maintenance Domain.")
public class CfmMdAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name-type",
            description = "Maintenance Domain name type",
            required = true, multiValued = false)
    String nameType = null;

    @Argument(index = 1, name = "name",
            description = "Maintenance Domain name. Restrictions apply depending " +
                    "on name-type. Leave empty if name type is none",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 2, name = "level",
            description = "Maintenance Domain level LEVEL0-LEVEL7",
            required = true, multiValued = false)
    String level = null;

    @Argument(index = 3, name = "numeric-id",
            description = "An optional numeric id for Maintenance Domain [1-65535]",
            required = false, multiValued = false)
    short numericId = 0;

    @Override
    protected void execute() {
        CfmMdService service = get(CfmMdService.class);
        MdId mdId = null;
        MdId.MdNameType nameTypeEnum = MdId.MdNameType.valueOf(nameType);
        switch (nameTypeEnum) {
            case DOMAINNAME:
                mdId = MdIdDomainName.asMdId(name);
                break;
            case MACANDUINT:
                mdId = MdIdMacUint.asMdId(name);
                break;
            case NONE:
                mdId = MdIdNone.asMdId();
                break;
            case CHARACTERSTRING:
            default:
                mdId = MdIdCharStr.asMdId(name);
        }
        MaintenanceDomain.MdLevel levelEnum =
                MaintenanceDomain.MdLevel.valueOf(level);
        Optional<Short> numericIdOpt = Optional.empty();
        if (numericId > 0) {
            numericIdOpt = Optional.of(numericId);
        }

        MaintenanceDomain.MdBuilder builder = null;
        try {
            builder = DefaultMaintenanceDomain.builder(mdId).mdLevel(levelEnum);
            if (numericIdOpt.isPresent()) {
                builder = builder.mdNumericId(numericIdOpt.get());
            }
            boolean created = service.createMaintenanceDomain(builder.build());
            print("Maintenance Domain with id %s is successfully %s.",
                    mdId, created ? "updated" : "created");
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
