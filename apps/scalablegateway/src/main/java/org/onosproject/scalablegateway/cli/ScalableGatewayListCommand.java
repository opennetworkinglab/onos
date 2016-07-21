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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.scalablegateway.api.ScalableGatewayService;

/**
 * Lists all gateway node information of scalablegateway.
 */

@Command(scope = "onos", name = "gateways",
        description = "Lists gateway node information")
public class ScalableGatewayListCommand extends AbstractShellCommand {

    private static final String FORMAT = "GatewayNode Id[%s]: DataPlane Ip[%s], External Interface names[%s]";
    @Override
    protected void execute() {
        ScalableGatewayService service = get(ScalableGatewayService.class);
        service.getGatewayNodes().forEach(node -> print(FORMAT,
                node.getGatewayDeviceId().toString(),
                node.getDataIpAddress().toString(),
                node.getUplinkIntf().toString()));
    }
}
