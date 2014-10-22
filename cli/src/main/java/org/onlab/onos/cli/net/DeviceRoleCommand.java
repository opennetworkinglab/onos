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
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.mastership.MastershipAdminService;
import org.onlab.onos.net.MastershipRole;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Sets role of the controller node for the given infrastructure device.
 */
@Command(scope = "onos", name = "device-role",
         description = "Sets role of the controller node for the given infrastructure device")
public class DeviceRoleCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = true, multiValued = false)
    String uri = null;

    @Argument(index = 1, name = "node", description = "Node ID",
              required = true, multiValued = false)
    String node = null;

    @Argument(index = 2, name = "role", description = "Mastership role",
              required = true, multiValued = false)
    String role = null;

    @Override
    protected void execute() {
        MastershipAdminService service = get(MastershipAdminService.class);
        MastershipRole mastershipRole = MastershipRole.valueOf(role.toUpperCase());
        service.setRole(new NodeId(node), deviceId(uri), mastershipRole);
    }

}
