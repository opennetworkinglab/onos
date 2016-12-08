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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.protection.ProtectedTransportEndpointDescription;
import org.onosproject.net.behaviour.protection.TransportEndpointDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.ProtectionEndpointIntent;

/**
 * Test tool to add ProtectionEndpointIntent.
 */
@Command(scope = "onos", name = "test-add-protection-endpoint",
         description = "Test tool to add ProtectionEndpointIntent")
public class TestProtectionEndpointIntentCommand extends AbstractShellCommand {

    private static final String DEFAULT_FINGERPRINT = "[fingerprint]";

    @Option(name = "--fingerprint",
            description = "Fingerprint to identify the protected transport entity",
            valueToShowInHelp = DEFAULT_FINGERPRINT)
    private String fingerprint = DEFAULT_FINGERPRINT;

    @Argument(index = 0, name = "deviceId",
            description = "Device ID to configure",
            required = true)
    private String deviceIdStr = null;

    @Argument(index = 1, name = "peerDeviceId",
            description = "Device ID of remote peer",
            required = true)
    private String peerStr = null;

    @Argument(index = 2, name = "portNumber1",
            description = "PortNumber leading to working path on deviceId",
            required = true)
    private String portNumber1Str = null;

    @Argument(index = 3, name = "portNumber2",
            description = "PortNumber leading to standby path on deviceId",
            required = true)
    private String portNumber2Str = null;

    @Option(name = "--vlan1",
            description = "VLAN ID to push on portNumber1 expressed in decimal")
    private String vlan1Str = null;

    @Option(name = "--vlan2",
            description = "VLAN ID to push on portNumber2 expressed in decimal")
    private String vlan2Str = null;

    private IntentService intentService;
    private DeviceService deviceService;

    @Override
    protected void execute() {
        fingerprint = Optional.ofNullable(fingerprint)
                              .orElse(DEFAULT_FINGERPRINT);

        intentService = get(IntentService.class);
        deviceService = get(DeviceService.class);

        DeviceId did = DeviceId.deviceId(deviceIdStr);
        DeviceId peer = DeviceId.deviceId(peerStr);

        ProtectedTransportEndpointDescription description;
        List<TransportEndpointDescription> paths = new ArrayList<>();

        paths.add(TransportEndpointDescription.builder()
                  .withOutput(output(did, portNumber1Str, vlan1Str))
                  .build());
        paths.add(TransportEndpointDescription.builder()
                  .withOutput(output(did, portNumber2Str, vlan2Str))
                  .build());

        description = ProtectedTransportEndpointDescription.of(paths, peer, fingerprint);

        ProtectionEndpointIntent intent;
        intent = ProtectionEndpointIntent.builder()
                .key(Key.of(fingerprint, appId()))
                .appId(appId())
                .deviceId(did)
                .description(description)
                .build();
        print("Submitting: %s", intent);
        intentService.submit(intent);
    }

    private FilteredConnectPoint output(DeviceId did, String portNumberStr, String vlanStr) {
        ConnectPoint cp = new ConnectPoint(did,
                                           PortNumber.fromString(portNumberStr));
        if (deviceService.getPort(cp) == null) {
            print("Unknown port: %s", cp);
        }

        if (vlanStr == null) {
            return new FilteredConnectPoint(cp);
        } else {
            VlanId vlan = VlanId.vlanId(vlanStr);
            TrafficSelector sel = DefaultTrafficSelector.builder()
                    .matchVlanId(vlan)
                    .build();

            return new FilteredConnectPoint(cp, sel);

        }
    }

}
