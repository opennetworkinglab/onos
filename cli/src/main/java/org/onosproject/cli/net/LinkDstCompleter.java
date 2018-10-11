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

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.cli.AbstractCompleter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.link.LinkService;

import java.util.List;
import java.util.SortedSet;

/**
 * Link destination end-point completer.
 */
@Service
public class LinkDstCompleter extends AbstractCompleter {
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Fetch our service and feed it's offerings to the string completer
        LinkService service = AbstractShellCommand.get(LinkService.class);

        // Link source the previous argument.
        String srcArg = commandLine.getArguments()[commandLine.getCursorArgumentIndex() - 1];

        // Generate the device ID/port number identifiers
        SortedSet<String> strings = delegate.getStrings();
        try {
            ConnectPoint src = ConnectPoint.deviceConnectPoint(srcArg);
            service.getEgressLinks(src)
                    .forEach(link -> strings.add(link.dst().elementId().toString() +
                                                         "/" + link.dst().port()));
        } catch (NumberFormatException e) {
            System.err.println("Invalid connect-point");
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }

}
