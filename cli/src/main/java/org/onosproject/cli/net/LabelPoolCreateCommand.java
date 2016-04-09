/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;

/**
 * create label resource pool by specific device id.
 */
@Command(scope = "onos", name = "label-pool-create",
     description = "Creates label resource pool by a specific device id")
public class LabelPoolCreateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId", description = "Device identity", required = true, multiValued = false)
    String deviceId = null;
    @Argument(index = 1, name = "beginLabel",
            description = "The first label of global label resource pool.", required = true, multiValued = false)
    String beginLabel = null;
    @Argument(index = 2, name = "endLabel",
            description = "The last label of global label resource pool.", required = true, multiValued = false)
    String endLabel = null;

    @Override
    protected void execute() {
        LabelResourceAdminService lrs = get(LabelResourceAdminService.class);
        lrs.createDevicePool(DeviceId.deviceId(deviceId), LabelResourceId
                .labelResourceId(Long.parseLong(beginLabel)), LabelResourceId
                .labelResourceId(Long.parseLong(endLabel)));
    }

}
