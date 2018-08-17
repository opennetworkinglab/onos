/*
 * Copyright 2015-present Open Networking Foundation
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
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.utils.Comparators;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;

import java.util.Collections;
import java.util.List;

/**
 * Lists all configured interfaces.
 */
@Service
@Command(scope = "onos", name = "interfaces",
        description = "Lists all configured interfaces.")
public class InterfacesListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%s: port=%s/%s";
    private static final String IP_FORMAT = " ips=";
    private static final String MAC_FORMAT = " mac=";
    private static final String VLAN_FORMAT = " vlan=";
    private static final String VLAN_UNTAGGED = " vlanUntagged=";
    private static final String VLAN_TAGGED = " vlanTagged=";
    private static final String VLAN_NATIVE = " vlanNative=";

    private static final String NO_NAME = "(unamed)";

    @Override
    protected void doExecute() {
        InterfaceService interfaceService = get(InterfaceService.class);

        List<Interface> interfaces = Lists.newArrayList(interfaceService.getInterfaces());

        Collections.sort(interfaces, Comparators.INTERFACES_COMPARATOR);

        interfaces.forEach(this::printInterface);
    }

    private void printInterface(Interface intf) {
        StringBuilder formatStringBuilder = new StringBuilder(FORMAT);

        if (!intf.ipAddressesList().isEmpty()) {
            formatStringBuilder.append(IP_FORMAT);
            formatStringBuilder.append(intf.ipAddressesList().toString());
        }

        if (!intf.mac().equals(MacAddress.NONE)) {
            formatStringBuilder.append(MAC_FORMAT);
            formatStringBuilder.append(intf.mac().toString());
        }

        if (!intf.vlan().equals(VlanId.NONE)) {
            formatStringBuilder.append(VLAN_FORMAT);
            formatStringBuilder.append(intf.vlan().toString());
        }

        if (!intf.vlanUntagged().equals(VlanId.NONE)) {
            formatStringBuilder.append(VLAN_UNTAGGED);
            formatStringBuilder.append(intf.vlanUntagged().toString());
        }

        if (!intf.vlanTagged().isEmpty()) {
            formatStringBuilder.append(VLAN_TAGGED);
            formatStringBuilder.append(intf.vlanTagged().toString());
        }

        if (!intf.vlanNative().equals(VlanId.NONE)) {
            formatStringBuilder.append(VLAN_NATIVE);
            formatStringBuilder.append(intf.vlanNative().toString());
        }

        String name = (intf.name().equals(Interface.NO_INTERFACE_NAME)) ?
                      NO_NAME : intf.name();

        print(formatStringBuilder.toString(), name, intf.connectPoint().deviceId(),
                intf.connectPoint().port());
    }

}
