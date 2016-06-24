/*
 * Copyright 2016-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.scalablegateway.api.GatewayNode;
import org.onosproject.scalablegateway.api.ScalableGatewayService;

/**
 * Deletes gateway node information for scalablegateway node managements.
 */

@Command(scope = "onos", name = "gateway-delete",
        description = "Deletes gateway node information for scalablegateway node managements")
public class ScalableGatewayDeleteCommand extends AbstractShellCommand {

    private static final String SUCCESS = "Process of deleting gateway node is succeed.";
    private static final String FAIL = "Process of deleting gateway node is failed.";
    private static final String UNKNOWN = "Unknown device id is given.";

    @Argument(index = 0, name = "DeviceId", description = "GatewayNode device id",
            required = true, multiValued = false)
    String deviceId = null;

    @Override
    protected void execute() {
        ScalableGatewayService service = get(ScalableGatewayService.class);

        GatewayNode gatewayNode = service.getGatewayNode(DeviceId.deviceId(deviceId));
        if (gatewayNode == null) {
            print(UNKNOWN);
            return;
        }

        if (service.deleteGatewayNode(gatewayNode)) {
            print(SUCCESS);
        } else {
            print(FAIL);
        }
    }

}
