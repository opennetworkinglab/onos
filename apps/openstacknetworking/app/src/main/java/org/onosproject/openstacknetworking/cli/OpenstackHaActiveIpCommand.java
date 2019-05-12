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
package org.onosproject.openstacknetworking.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackHaService;

/**
 * Configures openstack active IP address.
 */
@Service
@Command(scope = "onos", name = "openstack-ha-activeip",
        description = "Configure openstack active IP address.")
public class OpenstackHaActiveIpCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "active node IP", description = "active node IP",
            required = true, multiValued = false)
    private String ip = null;

    @Override
    protected void doExecute() {
        OpenstackHaService service = get(OpenstackHaService.class);

        service.setActiveIp(IpAddress.valueOf(ip));

        print("Active node IP address " + ip + " is configured");
    }
}
