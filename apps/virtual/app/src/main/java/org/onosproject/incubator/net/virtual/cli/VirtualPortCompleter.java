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

package org.onosproject.incubator.net.virtual.cli;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.incubator.net.virtual.Comparators;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.DeviceId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
/**
 * Virtual port completer.
 *
 * Assumes the first argument which can be parsed to a number is network id
 * and the argument right before the one being completed is device id
 */
@Service
public class VirtualPortCompleter extends AbstractChoicesCompleter {
    @Override
    protected List<String> choices() {
        //parse argument list for network id
        String[] argsArray = commandLine.getArguments();
        for (String str : argsArray) {
            if (str.matches("[0-9]+")) {
                long networkId = Long.valueOf(str);
                String deviceId = argsArray[argsArray.length - 1];
                return getSortedVirtualPorts(networkId, deviceId).stream()
                        .map(virtualPort -> virtualPort.number().toString())
                        .collect(Collectors.toList());
            }
        }

        return Collections.singletonList("Missing network id");
    }

    /**
     * Returns the list of virtual ports sorted using the port number.
     *
     * @param networkId network id.
     * @param deviceId device id
     * @return sorted virtual port list
     */
    private List<VirtualPort> getSortedVirtualPorts(long networkId, String deviceId) {
        VirtualNetworkService service = getService(VirtualNetworkService.class);

        List<VirtualPort> virtualPorts = new ArrayList<>();
        virtualPorts.addAll(service.getVirtualPorts(NetworkId.networkId(networkId),
                DeviceId.deviceId(deviceId)));
        Collections.sort(virtualPorts, Comparators.VIRTUAL_PORT_COMPARATOR);
        return virtualPorts;
    }

}
