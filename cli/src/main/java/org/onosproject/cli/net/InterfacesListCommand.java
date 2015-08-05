/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceService;

import java.util.Collections;
import java.util.List;

/**
 * Lists all configured interfaces.
 */
@Command(scope = "onos", name = "interfaces",
        description = "Lists all configured interfaces.")
public class InterfacesListCommand extends AbstractShellCommand {

    private static final String FORMAT =
            "port=%s/%s, ips=%s, mac=%s, vlan=%s";

    @Override
    protected void execute() {
        InterfaceService interfaceService = get(InterfaceService.class);

        List<Interface> interfaces = Lists.newArrayList(interfaceService.getInterfaces());

        Collections.sort(interfaces, Comparators.INTERFACES_COMPARATOR);

        for (Interface intf : interfaces) {
            print(FORMAT, intf.connectPoint().deviceId(), intf.connectPoint().port(),
                    intf.ipAddresses(), intf.mac(), intf.vlan());
        }
    }

}
