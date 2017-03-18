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

package org.onosproject.cli.net.vnet;

import org.apache.karaf.shell.console.completer.ArgumentCompleter.ArgumentList;
import org.onosproject.cli.AbstractChoicesCompleter;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.onlab.osgi.DefaultServiceDirectory.getService;

/**
 * Virtual host completer.
 *
 * Assumes the first argument which can be parsed to a number is network id.
 */
public class VirtualHostCompleter extends AbstractChoicesCompleter {
    @Override
    protected List<String> choices() {
        ArgumentList args = getArgumentList();
        //parse argument list for network id
        String[] argsArray = args.getArguments();
        for (String str : argsArray) {
            if (str.matches("[0-9]+")) {
                long networkId = Long.valueOf(str);
                return getSortedVirtualHosts(networkId).stream()
                        .map(virtualHost -> virtualHost.id().toString())
                        .collect(Collectors.toList());
            }
        }
        return Collections.singletonList("Missing network id");
    }

    /**
     * Returns the list of virtual hosts sorted using the host identifier.
     *
     * @param networkId network id
     * @return virtual host list
     */
    private List<VirtualHost> getSortedVirtualHosts(long networkId) {
        VirtualNetworkService service = getService(VirtualNetworkService.class);

        List<VirtualHost> virtualHosts = new ArrayList<>();
        virtualHosts.addAll(service.getVirtualHosts(NetworkId.networkId(networkId)));
        return virtualHosts;
    }

}
