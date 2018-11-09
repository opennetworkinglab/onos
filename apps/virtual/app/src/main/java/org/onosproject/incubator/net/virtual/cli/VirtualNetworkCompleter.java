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

import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.Comparators;
import org.onosproject.net.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.SortedSet;

/**
 * Virtual network completer.
 */
@Service
public class VirtualNetworkCompleter implements Completer {
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        VirtualNetworkAdminService service = AbstractShellCommand.get(VirtualNetworkAdminService.class);

        List<VirtualNetwork> virtualNetworks = new ArrayList<>();

        Set<TenantId> tenantSet = service.getTenantIds();
        tenantSet.forEach(tenantId -> virtualNetworks.addAll(service.getVirtualNetworks(tenantId)));

        Collections.sort(virtualNetworks, Comparators.VIRTUAL_NETWORK_COMPARATOR);

        SortedSet<String> strings = delegate.getStrings();
        virtualNetworks.forEach(virtualNetwork -> strings.add(virtualNetwork.id().toString()));

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }
}
