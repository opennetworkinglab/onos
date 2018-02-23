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

package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.segmentrouting.SegmentRoutingService;

/**
 * Triggers the verification of hashed group buckets in the specified device,
 * and corrects the buckets if necessary. Outcome can be viewed in the 'groups'
 * command.
 */
@Command(scope = "onos", name = "sr-verify-groups",
        description = "Triggers the verification of hashed groups in the specified "
                + "device. Does not return any output; users can query the results "
                + "in the 'groups' command")
public class VerifyGroupsCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        if (uri != null) {
            Device dev = deviceService.getDevice(DeviceId.deviceId(uri));
            if (dev != null) {
                srService.verifyGroups(dev.id());
            }
        }
    }
}
