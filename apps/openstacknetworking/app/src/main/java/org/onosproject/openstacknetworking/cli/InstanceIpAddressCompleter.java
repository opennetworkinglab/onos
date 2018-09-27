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

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortService;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

/**
 * Instance port ip address completer.
 */
@Service
public class InstanceIpAddressCompleter implements Completer {
    private static final String EXTERNAL_IP = "8.8.8.8";

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        InstancePortService instancePortService =
                AbstractShellCommand.get(InstancePortService.class);

        Set<IpAddress> set = instancePortService.instancePorts().stream()
                .map(InstancePort::ipAddress)
                .collect(Collectors.toSet());
        set.add(IpAddress.valueOf(EXTERNAL_IP));

        SortedSet<String> strings = delegate.getStrings();

        Iterator<IpAddress> it = set.iterator();

        while (it.hasNext()) {
            strings.add(it.next().toString());
        }

        return delegate.complete(session, commandLine, candidates);


    }
}
