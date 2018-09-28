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

package org.onosproject.scalablegateway.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.Ip4Address;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.scalablegateway.api.GatewayNode;
import org.onosproject.scalablegateway.api.ScalableGatewayService;

/**
 * Adds gateway node information for scalablegateway node managements.
 */
@Service
@Command(scope = "onos", name = "gateway-add",
        description = "Adds gateway node information for scalablegateway node managements")
public class ScalableGatewayAddCommand extends AbstractShellCommand {

    private static final String SUCCESS = "Process of adding gateway node is succeed";
    private static final String FAIL = "Process of adding gateway node is failed";

    @Argument(index = 0, name = "DeviceId", description = "GatewayNode device id",
            required = true, multiValued = false)
    String deviceId = null;

    @Argument(index = 1, name = "dataPlaneIp",
            description = "GatewayNode datePlane interface ip address",
            required = true, multiValued = false)
    String ipAddress = null;

    @Argument(index = 2, name = "extInterfaceNames",
            description = "GatewayNode Interface name to outgoing external network",
            required = true, multiValued = true)
    String interfaceName = null;

    @Override
    protected void doExecute() {
        ScalableGatewayService service = get(ScalableGatewayService.class);

        GatewayNode gatewayNode = GatewayNode.builder()
                .gatewayDeviceId(DeviceId.deviceId(deviceId))
                .dataIpAddress(Ip4Address.valueOf(ipAddress))
                .uplinkIntf(interfaceName)
                .build();
        if (service.addGatewayNode(gatewayNode)) {
            print(SUCCESS);
        } else {
            print(FAIL);
        }
    }

}
