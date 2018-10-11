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
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.StatisticService;


import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Fetches statistics.
 */
@Service
@Command(scope = "onos", name = "get-stats",
         description = "Fetches stats for a connection point")
public class GetStatisticsCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "connectPoint",
              description = "Device/Port Description",
              required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    String connectPoint = null;


    @Override
    protected void doExecute() {
        StatisticService service = get(StatisticService.class);

        DeviceId ingressDeviceId = deviceId(getDeviceId(connectPoint));
        PortNumber ingressPortNumber = portNumber(getPortNumber(connectPoint));
        ConnectPoint cp = new ConnectPoint(ingressDeviceId, ingressPortNumber);

        Load load = service.load(cp);

        print("Load on %s -> %s", cp, load);
    }

    /**
     * Extracts the port number portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return port number as a string, empty string if the port is not found
     */
    private String getPortNumber(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(slash + 1, deviceString.length());
    }

    /**
     * Extracts the device ID portion of the ConnectPoint.
     *
     * @param deviceString string representing the device/port
     * @return device ID string
     */
    private String getDeviceId(String deviceString) {
        int slash = deviceString.indexOf('/');
        if (slash <= 0) {
            return "";
        }
        return deviceString.substring(0, slash);
    }
}
