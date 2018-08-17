/*
 * Copyright 2016-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyAdminService;
import org.onosproject.net.key.DeviceKeyId;

/**
 * Adds a device key.
 */
@Service
@Command(scope = "onos", name = "device-key-add",
        description = "Adds a device key. Adding a new device key with " +
                "the same id will replace the existing device key.")

public class DeviceKeyAddCommand extends AbstractShellCommand {

    private static final String COMMUNITY_NAME = "CommunityName";
    private static final String USERNAME = "UsernamePassword";

    @Argument(index = 0, name = "id", description = "Device Key ID",
            required = true, multiValued = false)
    String id = null;

    @Argument(index = 1, name = "type", description = "Device Key Type, " +
            "it includes CommunityName, UsernamePassword.",
            required = true, multiValued = false)
    String type = null;

    @Option(name = "-c", aliases = "--communityName", description = "Device Key Community Name",
            required = false, multiValued = false)
    String communityName = null;

    @Option(name = "-l", aliases = "--label", description = "Device Key Label",
            required = false, multiValued = false)
    String label = null;

    @Option(name = "-u", aliases = "--username", description = "Device Key Username",
            required = false, multiValued = false)
    String username = null;

    @Option(name = "-p", aliases = "--password", description = "Device Key Password",
            required = false, multiValued = false)
    String password = null;

    @Override
    protected void doExecute() {
        DeviceKeyAdminService service = get(DeviceKeyAdminService.class);
        DeviceKey deviceKey = null;
        if (type.equalsIgnoreCase(COMMUNITY_NAME)) {
            deviceKey = DeviceKey.createDeviceKeyUsingCommunityName(DeviceKeyId.deviceKeyId(id),
                                                                    label, communityName);
        } else if (type.equalsIgnoreCase(USERNAME)) {
            deviceKey = DeviceKey.createDeviceKeyUsingUsernamePassword(DeviceKeyId.deviceKeyId(id),
                                                                       label, username, password);
        } else {
            print("Invalid Device key type: {}", type);
            return;
        }
        service.addKey(deviceKey);
        print("Device Key successfully added.");
    }
}
