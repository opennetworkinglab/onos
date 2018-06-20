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
package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.xconnect.api.XconnectService;

/**
 * Deletes Xconnect.
 */
@Command(scope = "onos", name = "sr-xconnect-remove", description = "Remove Xconnect")
public class XconnectRemoveCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    private String deviceIdStr;

    @Argument(index = 1, name = "vlanId",
            description = "VLAN ID",
            required = true, multiValued = false)
    private String vlanIdStr;

    @Override
    protected void execute() {
        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);
        VlanId vlanId = VlanId.vlanId(vlanIdStr);

        XconnectService xconnectService = get(XconnectService.class);
        xconnectService.removeXonnect(deviceId, vlanId);
    }
}
