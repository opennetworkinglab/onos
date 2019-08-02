/*
 * Copyright 2019-present Open Networking Foundation
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


package org.onosproject.net.optical.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.OpticalConnectPointCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.device.DeviceService;

/**
 * Lists all the available wavelengths (lambdas) for a given port.
 */
@Service
@Command(scope = "onos", name = "available-wavelength",
        description = "Lists all the available wavelengths for a given port")
public class PortAvailableWaveLengthCommand extends AbstractShellCommand {

    private static final String FMT =
            "signal=%s, central-frequency=%f";

    @Argument(index = 0, name = "connectPoint",
            description = "Device/Port Description",
            required = true, multiValued = false)
    @Completion(OpticalConnectPointCompleter.class)
    String connectPointString = "";


    @Override
    protected void doExecute() throws Exception {
        DeviceService deviceService = get(DeviceService.class);
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(connectPointString);

        Device d = deviceService.getDevice(cp.deviceId());
        if (d.is(LambdaQuery.class)) {
            LambdaQuery lambdaQuery = d.as(LambdaQuery.class);
            lambdaQuery.queryLambdas(cp.port()).forEach(lambda -> {
                print(FMT, lambda.toString(), lambda.centralFrequency().asGHz());
            });

        } else {
            print("Device is not capable of querying lambdas");
        }

    }
}
