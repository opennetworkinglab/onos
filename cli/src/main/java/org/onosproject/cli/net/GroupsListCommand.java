/*
 * Copyright 2015 Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupService;

/**
 * Lists all groups in the system.
 */
@Command(scope = "onos", name = "groups",
        description = "Lists all groups in the system")
public class GroupsListCommand extends AbstractShellCommand {

    private static final String FORMAT =
            "   key=%s, id=%s, state=%s, bytes=%s, packets=%s, appId=%s, buckets=%s";

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        GroupService groupService = get(GroupService.class);

        deviceService.getDevices().forEach(d ->
                printGroups(d.id(), groupService.getGroups(d.id()))
        );
    }

    private void printGroups(DeviceId deviceId, Iterable<Group> groups) {
        print("deviceId=%s", deviceId);
        for (Group group : groups) {
            print(FORMAT, group.appCookie(), group.id(), group.state(),
                  group.bytes(), group.packets(), group.appId(), group.buckets());
        }
    }
}
