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
import org.onlab.packet.VlanId;
import org.onosproject.cfm.cli.completer.CfmMaCcmIntervalCompleter;
import org.onosproject.cfm.cli.completer.CfmMaNameTypeCompleter;
import org.onosproject.cfm.cli.completer.CfmMdNameCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.PlaceholderCompleter;
import org.onosproject.incubator.net.l2monitoring.cfm.Component;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultComponent;
import org.onosproject.incubator.net.l2monitoring.cfm.DefaultMaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;

/**
 * Adds a Maintenance Association to a Maintenance Domain.
 */
@Service
@Command(scope = "onos", name = "cfm-ma-add",
        description = "Add a CFM Maintenance Association to a Maintenance Domain.")
public class CfmMaAddCommand extends AbstractShellCommand {
    @Argument(name = "name",
            description = "Maintenance Domain name and type (in brackets)",
            required = true)
    @Completion(CfmMdNameCompleter.class)
    private String mdName = null;

    @Argument(index = 1, name = "name-type",
            description = "Maintenance Association name type",
            required = true)
    @Completion(CfmMaNameTypeCompleter.class)
    private String nameType = null;

    @Argument(index = 2, name = "name",
            description = "Maintenance Assocation name. Restrictions apply depending " +
                    "on name-type",
            required = true)
    @Completion(PlaceholderCompleter.class)
    private String name = null;

    @Argument(index = 3, name = "ccm-interval",
            description = "CCM Interval values from list",
            required = true)
    @Completion(CfmMaCcmIntervalCompleter.class)
    private String ccmInterval = null;

    @Argument(index = 4, name = "numeric-id",
            description = "An optional numeric id for Maintenance Association [1-65535]")
    private Short numericId = null;

    @Argument(index = 5, name = "component-id",
            description = "An id for a Component in the Component List [1-65535]." +
                    "The CLI allows creation of only 1 component. If more are " +
                    "required use the REST interface")
    private Short componentId = null;

    @Argument(index = 6, name = "component-tag-type",
            description = "Tag Type value for the component")
    private String tagType = null;

    @Argument(index = 7, name = "component-mhf-creation",
            description = "MEP Half function creation type for the component")
    private String mhfCreationType = null;

    @Argument(index = 8, name = "component-vid",
            description = "A VID for the component [1-4095]. This CLI allows " +
                    "only the specification of 1 VID. If more are required use " +
                    "the REST interface")
    private Short vid = null;

    @Argument(index = 9, name = "rmep",
            description = "Remote Mep numeric identifier [1-8192]",
            required = true, multiValued = true)
    private String[] rmepArray = null;

    @Override
    protected void doExecute() {
        CfmMdService service = get(CfmMdService.class);

        String[] mdNameParts = mdName.split("[()]");
        if (mdNameParts.length != 2) {
            throw new IllegalArgumentException("Invalid name format. " +
                    "Must be in the format of <identifier(name-type)>");
        }

        MdId mdId = CfmMdListMdCommand.parseMdName(mdNameParts[0] + "(" + mdNameParts[1] + ")");

        MaIdShort maId = CfmMdListMdCommand.parseMaName(name + "(" + nameType + ")");

        try {
            MaintenanceAssociation.MaBuilder builder = DefaultMaintenanceAssociation
                    .builder(maId, mdId.getNameLength());
            if (ccmInterval != null && !ccmInterval.isEmpty()) {
                builder = builder.ccmInterval(MaintenanceAssociation.CcmInterval.valueOf(ccmInterval));
            }
            for (String rmep:rmepArray) {
                builder = builder.addToRemoteMepIdList(MepId.valueOf(Short.parseShort(rmep)));
            }
            if (numericId != null) {
                builder = builder.maNumericId(numericId);
            }
            if (componentId != null) {
                Component.ComponentBuilder compBuilder =
                        DefaultComponent.builder(componentId);
                if (tagType != null && !tagType.isEmpty()) {
                    compBuilder = compBuilder.tagType(
                            Component.TagType.valueOf(tagType));
                }
                if (mhfCreationType != null && !mhfCreationType.isEmpty()) {
                    compBuilder = compBuilder.mhfCreationType(
                            Component.MhfCreationType.valueOf(mhfCreationType));
                }
                if (vid != null) {
                    compBuilder = compBuilder.addToVidList(VlanId.vlanId(vid));
                }
                builder = builder.addToComponentList(compBuilder.build());
            }
            boolean created = service.createMaintenanceAssociation(mdId, builder.build());
            print("Maintenance Association %s is successfully %s on MD %s",
                    maId, created ? "updated" : "created", mdId);
        } catch (CfmConfigException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
