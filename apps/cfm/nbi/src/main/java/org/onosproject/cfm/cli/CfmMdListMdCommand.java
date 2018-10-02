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
import org.onosproject.cfm.cli.completer.CfmMdNameCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdMaNameUtil;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

/**
 * Lists a particular Maintenance Domain.
 */
@Service
@Command(scope = "onos", name = "cfm-md-list",
        description = "Lists a single CFM Maintenance Domain or all if none specified.")
public class CfmMdListMdCommand extends AbstractShellCommand {
    @Argument(name = "name",
            description = "Maintenance Domain name and type (in brackets)")
    @Completion(CfmMdNameCompleter.class)
    private String name = null;

    @Override
    protected void doExecute() {
        CfmMdService service;
        service = get(CfmMdService.class);

        if (name != null) {
            MdId mdId = parseMdName(name);
            print("Maintenance Domain:");
            service.getMaintenanceDomain(mdId).ifPresent(md -> {
                print(printMd(md));
                md.maintenanceAssociationList().forEach(CfmMdListMdCommand::printMa);
            });
        } else {
            service.getAllMaintenanceDomain().forEach(md -> print(printMd(md)));
        }
    }

    private static String printMd(MaintenanceDomain md) {
        if (md == null) {
            return "MD not found";
        } else {
            StringBuilder sb = new StringBuilder("\tMD: ");
            sb.append(md.mdId().mdName());
            sb.append("(");
            sb.append(md.mdId().nameType());
            sb.append(") Lvl:");
            sb.append(md.mdLevel().ordinal());
            sb.append(", Num: ");
            sb.append(md.mdNumericId());

            md.maintenanceAssociationList().
                    forEach(ma -> sb.append(printMa(ma)));
            return sb.toString();
        }
    }

    private static String printMa(MaintenanceAssociation ma) {
        if (ma == null) {
            return "\n\tNo MA found";
        }

        StringBuilder sb = new StringBuilder("\n\t\tMA: ");
        sb.append(ma.maId().maName());
        sb.append("(");
        sb.append(ma.maId().nameType());
        sb.append(") CCM: ");
        sb.append(ma.ccmInterval());
        sb.append(" Num: ");
        sb.append(ma.maNumericId());

        ma.remoteMepIdList().forEach(rmep -> {
                    sb.append("\n\t\t\tRmep: ");
                    sb.append(rmep);
                });
        ma.componentList().forEach(comp -> {
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

    static MdId parseMdName(String mdStr) {
        String[] nameParts = mdStr.split("[()]");
        if (nameParts.length != 2) {
            throw new IllegalArgumentException("Invalid name format. " +
                    "Must be in the format of <identifier(name-type)>");
        }

        return MdMaNameUtil.parseMdName(nameParts[1], nameParts[0]);
    }

    static MaIdShort parseMaName(String maStr) {
        String[] nameParts = maStr.split("[()]");
        if (nameParts.length != 2) {
            throw new IllegalArgumentException("Invalid name format. " +
                    "Must be in the format of <identifier(name-type)>");
        }

        return MdMaNameUtil.parseMaName(nameParts[1], nameParts[0]);
    }

}
