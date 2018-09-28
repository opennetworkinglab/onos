/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.dhcprelay.cli;

//import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.apache.karaf.shell.api.action.Argument;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.IpAddress;
import org.onosproject.routing.fpm.api.FpmRecord;
import org.onosproject.dhcprelay.api.DhcpRelayService;

/**
 * Prints Dhcp FPM Routes information.
 */
@Service
@Command(scope = "onos", name = "dhcp-fpm-add",
         description = "Add DHCP FPM prefix in dhcp-fpm-store.")
public class DhcpFpmAddCommand extends AbstractShellCommand {

    private static final DhcpRelayService DHCP_RELAY_SERVICE = get(DhcpRelayService.class);

    @Argument(index = 0, name = "prefix",
            description = "prefix",
            required = true, multiValued = false)
    String prefixString = null;

    @Argument(index = 1, name = "next hop",
            description = "next hop",
            required = true, multiValued = false)
    String nextHopString = null;

    @Override
    protected void doExecute() {

        IpPrefix prefix = IpPrefix.valueOf(prefixString);
        IpAddress nextHop = IpAddress.valueOf(nextHopString);
        FpmRecord record = new FpmRecord(prefix, nextHop, FpmRecord.Type.DHCP_RELAY);

        DHCP_RELAY_SERVICE.addFpmRecord(prefix, record);
    }
}
