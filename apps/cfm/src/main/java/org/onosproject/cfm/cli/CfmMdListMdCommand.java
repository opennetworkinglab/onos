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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

import java.util.Optional;

/**
 * Lists a particular Maintenance Domain.
 */
@Command(scope = "onos", name = "cfm-md-list",
        description = "Lists a single CFM Maintenance Domain or all if none specified.")
public class CfmMdListMdCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "name",
            description = "Maintenance Domain name and type (in brackets)",
            required = false, multiValued = false)
    String name = null;

    @Override
    protected void execute() {
        CfmMdService service = get(CfmMdService.class);

        if (name != null) {
            String[] nameParts = name.split("[()]");
            if (nameParts.length != 2) {
                throw new IllegalArgumentException("Invalid name format. " +
                        "Must be in the format of <identifier(name-type)>");
            }

            MdId mdId = null;
            MdId.MdNameType nameTypeEnum = MdId.MdNameType.valueOf(nameParts[1]);
            switch (nameTypeEnum) {
                case DOMAINNAME:
                    mdId = MdIdDomainName.asMdId(nameParts[0]);
                    break;
                case MACANDUINT:
                    mdId = MdIdMacUint.asMdId(nameParts[0]);
                    break;
                case NONE:
                    mdId = MdIdNone.asMdId();
                    break;
                case CHARACTERSTRING:
                default:
                    mdId = MdIdCharStr.asMdId(nameParts[0]);
            }

            print("Maintenance Domain:");
            Optional<MaintenanceDomain> md = service.getMaintenanceDomain(mdId);
            print(printMd(md));
            md.get().maintenanceAssociationList().forEach(ma -> printMa(Optional.of(ma)));
        } else {
            service.getAllMaintenanceDomain().forEach(md -> {
                print(printMd(Optional.of(md)));
            });
        }
    }

    public static String printMd(Optional<MaintenanceDomain> md) {
        if (!md.isPresent()) {
            return new String("MD not found");
        } else {
            StringBuffer sb = new StringBuffer("\tMD: ");
            sb.append(md.get().mdId().mdName());
            sb.append("(" + md.get().mdId().nameType());
            sb.append(") Lvl:" + md.get().mdLevel().ordinal());
            sb.append(", Num: " + md.get().mdNumericId());

            md.get().maintenanceAssociationList().
                    forEach(ma -> sb.append(printMa(Optional.of(ma))));
            return sb.toString();
        }
    }

    public static String printMa(Optional<MaintenanceAssociation> ma) {
        if (!ma.isPresent()) {
            return "\n\tNo MA found";
        }

        StringBuilder sb = new StringBuilder("\n\t\tMA: ");
        sb.append(ma.get().maId().maName());
        sb.append("(");
        sb.append(ma.get().maId().nameType());
        sb.append(") CCM: ");
        sb.append(ma.get().ccmInterval());
        sb.append(" Num: ");
        sb.append(ma.get().maNumericId());

        ma.get().remoteMepIdList().forEach(rmep -> {
                    sb.append("\n\t\t\tRmep: ");
                    sb.append(rmep);
                });
        ma.get().componentList().forEach(comp -> {
            sb.append("\n\t\t\tComponent: ");
            sb.append(comp.componentId());
            sb.append(" Perm: ");
            sb.append(comp.idPermission());
            sb.append(" MHF: ");
            sb.append(comp.mhfCreationType());
            sb.append(" Tag: ");
            sb.append(comp.tagType());

            comp.vidList().forEach(vid -> {
                    sb.append("\n\t\t\t\tVID: ");
                    sb.append(vid);
            });
        });

        return sb.toString();
    }
}
