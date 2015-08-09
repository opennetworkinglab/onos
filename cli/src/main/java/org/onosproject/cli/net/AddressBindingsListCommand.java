/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.Comparators;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.host.PortAddresses;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Lists all configured address port bindings.
 */
@Command(scope = "onos", name = "address-bindings",
        description = "Lists all configured address port bindings.")
public class AddressBindingsListCommand extends AbstractShellCommand {

    private static final String FORMAT =
            "port=%s/%s, ip(s)=%s, mac=%s, vlan=%s";

    @Override
    protected void execute() {
        HostService hostService = get(HostService.class);

        List<PortAddresses> addresses =
                Lists.newArrayList(hostService.getAddressBindings());

        Collections.sort(addresses, Comparators.ADDRESSES_COMPARATOR);

        for (PortAddresses pa : addresses) {
            print(FORMAT, pa.connectPoint().deviceId(), pa.connectPoint().port(),
                    printIpAddresses(pa.ipAddresses()), pa.mac(), pa.vlan());
        }
    }

    private String printIpAddresses(Set<InterfaceIpAddress> addresses) {
        StringBuilder output = new StringBuilder("[");
        for (InterfaceIpAddress address : addresses) {
            output.append(address.ipAddress().toString());
            output.append("/");
            output.append(address.subnetAddress().prefixLength());
            output.append(", ");
        }
        // Remove the last comma
        output.delete(output.length() - 2 , output.length());
        output.append("]");
        return output.toString();
    }

}
