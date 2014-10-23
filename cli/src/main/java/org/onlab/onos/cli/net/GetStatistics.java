/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;

import org.onlab.onos.net.statistic.Load;
import org.onlab.onos.net.statistic.StatisticService;


import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

/**
 * Fetches statistics.
 */
@Command(scope = "onos", name = "get-stats",
         description = "Fetches stats for a connection point")
public class GetStatistics extends AbstractShellCommand {

    @Argument(index = 0, name = "connectPoint",
              description = "Device/Port Description",
              required = true, multiValued = false)
    String connectPoint = null;


    @Override
    protected void execute() {
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
