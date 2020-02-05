/*
 * Copyright 2020-present Open Networking Foundation
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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BitErrorRateState;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Get the bit-error-rate for specific optical-channel.
 * This is a command for BitErrorRate.
 */
@Service
@Command(scope = "onos", name = "bit-error-rate",
        description = "Get ber for specific optical-channel")
public class BitErrorCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(BitErrorCommand.class);

    @Argument(index = 0, name = "connection point", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    private String connectPoint = null;

    @Override
    protected void doExecute() throws Exception {
        DeviceService deviceService = get(DeviceService.class);
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(connectPoint);
        Port port = deviceService.getPort(cp);
        if (port == null) {
            print("[ERROR] %s does not exist", cp);
            return;
        }

        Device device = deviceService.getDevice(cp.deviceId());
        BitErrorRateState bitErrorRateState = device.as(BitErrorRateState.class);
        Optional<Double> preFecBerVal = bitErrorRateState.getPreFecBer(cp.deviceId(), cp.port());
        if (preFecBerVal.isPresent()) {
            double preFecBer = preFecBerVal.orElse(Double.MIN_VALUE);
            print("The pre-fec-ber value in port %s on device %s is %f.",
                  cp.port().toString(), cp.deviceId().toString(), preFecBer);
        }
        Optional<Double> postFecBerVal = bitErrorRateState.getPostFecBer(cp.deviceId(), cp.port());
        if (postFecBerVal.isPresent()) {
            double postFecBer = postFecBerVal.orElse(Double.MIN_VALUE);
            print("The post-fec-ber value in port %s on device %s is %f.",
                  cp.port().toString(), cp.deviceId().toString(), postFecBer);
        }
    }
}
