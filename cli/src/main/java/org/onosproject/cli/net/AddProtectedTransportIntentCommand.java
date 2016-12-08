/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.ProtectedTransportIntent;

/**
 * Installs ProtectedTransportIntent.
 */
@Command(scope = "onos", name = "add-protected-transport",
         description = "Adds ProtectedTransportIntent")
public class AddProtectedTransportIntentCommand
    extends AbstractShellCommand {

    @Option(name = "--key", aliases = "-k",
            description = "Intent key",
            required = false, multiValued = false)
    private String keyStr = null;

    @Argument(index = 0, name = "deviceId1",
            description = "First Device ID of protected path",
            required = true, multiValued = false)
    private String deviceId1Str = null;

    @Argument(index = 1, name = "deviceId2",
            description = "Second Device ID of protected path",
            required = true, multiValued = false)
    private String deviceId2Str = null;

    private IntentService intentService;

    @Override
    protected void execute() {
        intentService = get(IntentService.class);

        DeviceId did1 = DeviceId.deviceId(deviceId1Str);
        DeviceId did2 = DeviceId.deviceId(deviceId2Str);

        Intent intent;
        intent = ProtectedTransportIntent.builder()
                .key(key())
                .appId(appId())
                .one(did1)
                .two(did2)
                .build();

        print("Submitting: %s", intent);
        intentService.submit(intent);
    }

    /**
     * Returns Key if specified in the option.
     *
     * @return Key instance or null
     */
    protected Key key() {
        return Optional.ofNullable(keyStr)
                    .map(s -> Key.of(s, appId()))
                    .orElse(null);
    }
}
