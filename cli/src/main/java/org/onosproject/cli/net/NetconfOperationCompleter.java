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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import java.util.List;
import java.util.SortedSet;

/**
 * Netconf Operation Completer.
 */
@Service
public class NetconfOperationCompleter implements Completer {

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();
        SortedSet<String> strings = delegate.getStrings();
        // The available operations are defined in NETCONF protocol (https://tools.ietf.org/html/rfc6241#section-7).
        strings.add("get");
        strings.add("get-config");
        strings.add("edit-config");
        strings.add("copy-config");
        strings.add("delete-config");
        strings.add("lock");
        strings.add("unlock");
        strings.add("close-session");
        strings.add("kill-session");
        return delegate.complete(session, commandLine, candidates);
    }

}
