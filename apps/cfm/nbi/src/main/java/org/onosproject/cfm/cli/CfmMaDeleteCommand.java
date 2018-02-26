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
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaId2Octet;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdIccY1731;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdPrimaryVid;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdRfc2685VpnId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdDomainName;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdMacUint;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdNone;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

/**
 * Delete a Maintenance Association from the existing list of a Maintenance Domain.
 */
@Command(scope = "onos", name = "cfm-ma-delete",
        description = "Delete a CFM Maintenance Association and its children.")
public class CfmMaDeleteCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "Maintenance Domain name and type (in brackets) " +
                    "and the Maintenance Association name and type (in brackets)",
            required = true, multiValued = false)
    String name = null;

    @Override
    protected void execute() {
        CfmMdService service = get(CfmMdService.class);

        String[] nameParts = name.split("[()]");
        if (nameParts.length != 4) {
            throw new IllegalArgumentException("Invalid name format. Must be in " +
                    "the format of <identifier(name-type)identifier(name-type)>");
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

        MaIdShort maId = null;
        MaIdShort.MaIdType maNameTypeEnum = MaIdShort.MaIdType.valueOf(nameParts[3]);
        switch (maNameTypeEnum) {
            case TWOOCTET:
                maId = MaId2Octet.asMaId(nameParts[2]);
                break;
            case ICCY1731:
                maId = MaIdIccY1731.asMaId(nameParts[2]);
                break;
            case PRIMARYVID:
                maId = MaIdPrimaryVid.asMaId(nameParts[2]);
                break;
            case RFC2685VPNID:
                maId = MaIdRfc2685VpnId.asMaIdHex(nameParts[2]);
                break;
            case CHARACTERSTRING:
            default:
                maId = MaIdCharStr.asMaId(nameParts[2]);
        }

        try {
            boolean deleted = service.deleteMaintenanceAssociation(mdId, maId);
            print("Maintenance Association %s-%s is %ssuccessfully deleted.",
                    mdId, maId, deleted ? "" : "NOT ");
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
