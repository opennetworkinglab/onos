/*
 * Copyright 2018-present Open Networking Foundation
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

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * Vlan Id Completer.
 */
public class VlanIdCompleter implements Completer {

    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        OpenstackNetworkService osNetService = AbstractShellCommand.get(OpenstackNetworkService.class);
        Set<VlanId> set = osNetService.externalPeerRouters().stream()
                .map(ExternalPeerRouter::externalPeerRouterVlanId)
                .collect(Collectors.toSet());
        SortedSet<String> strings = delegate.getStrings();

        Iterator<VlanId> it = set.iterator();

        while (it.hasNext()) {
            strings.add(it.next().toString());
        }

        return delegate.complete(buffer, cursor, candidates);

    }
}