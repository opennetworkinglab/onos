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

import org.onosproject.dhcprelay.api.DhcpRelayService;
import org.onosproject.routing.fpm.api.FpmRecord;

import java.util.Collection;

/**
 * Prints Dhcp FPM Routes information.
 */
@Service
@Command(scope = "onos", name = "dhcp-fpm-routes",
         description = "DHCP FPM routes cli.")
public class DhcpFpmRoutesCommand extends AbstractShellCommand {
    private static final String NO_RECORDS = "No DHCP FPM Route record found";
    private static final String HEADER = "DHCP FPM Routes records :";
    private static final String ROUTE = "prefix=%s, next-hop=%s";


    private static final DhcpRelayService DHCP_RELAY_SERVICE = get(DhcpRelayService.class);

    @Override
    protected void doExecute() {

            print("Dhcp Fpm Feature is %s !", DHCP_RELAY_SERVICE.isDhcpFpmEnabled() ? "enabled" : "disabled");
            print("\n");
            Collection<FpmRecord> records = DHCP_RELAY_SERVICE.getFpmRecords();
            if (records.isEmpty()) {
                print(NO_RECORDS);
                return;
            }
            print(HEADER);
            records.forEach(record -> print(ROUTE,
                    record.ipPrefix(),
                    record.nextHop()));
    }
}
