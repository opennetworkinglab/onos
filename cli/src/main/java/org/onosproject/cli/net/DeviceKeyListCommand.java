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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.utils.Comparators;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyService;

import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Lists all device keys.
 */
@Service
@Command(scope = "onos", name = "device-keys",
        description = "Lists all device keys")

public class DeviceKeyListCommand extends AbstractShellCommand {
    private static final String FMT_COMMUNITY_NAME =
            "identifier=%s, type=%s, community name=%s";
    private static final String FMT_USERNAME_PASSWORD =
            "identifier=%s, type=%s, username=%s, password=%s";

    @Override
    protected void doExecute() {
        DeviceKeyService service = get(DeviceKeyService.class);
        for (DeviceKey deviceKey : getSortedDeviceKeys(service)) {
            printDeviceKey(deviceKey);
        }
    }

    /**
     * Returns the list of devices keys sorted using the device key identifier.
     *
     * @param service device key service
     * @return sorted device key list
     */
    protected List<DeviceKey> getSortedDeviceKeys(DeviceKeyService service) {
        List<DeviceKey> deviceKeys = newArrayList(service.getDeviceKeys());
        Collections.sort(deviceKeys, Comparators.DEVICE_KEY_COMPARATOR);
        return deviceKeys;
    }

    /**
     * Prints out each device key.
     *
     * @param deviceKey the device key to be printed
     */
    private void printDeviceKey(DeviceKey deviceKey) {
        if (DeviceKey.Type.COMMUNITY_NAME.equals(deviceKey.type())) {
            print(FMT_COMMUNITY_NAME, deviceKey.deviceKeyId().id(), deviceKey.type(),
                  deviceKey.asCommunityName().name());
        } else if (DeviceKey.Type.USERNAME_PASSWORD.equals(deviceKey.type())) {
            print(FMT_USERNAME_PASSWORD, deviceKey.deviceKeyId().id(), deviceKey.type(),
                  deviceKey.asUsernamePassword().username(), deviceKey.asUsernamePassword().password());
        } else {
            log.error("Unsupported device key type: {}" + deviceKey.type());
        }
    }

}
