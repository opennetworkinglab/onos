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

package org.onosproject.cli.security;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.cli.AbstractCompleter;
import org.onosproject.core.Application;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import static org.onosproject.app.ApplicationState.INSTALLED;
import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Application name completer for security review command.
 */
@Service
public class ReviewApplicationNameCompleter extends AbstractCompleter {
    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        ApplicationService service = get(ApplicationService.class);
        Iterator<Application> it = service.getApplications().iterator();
        SortedSet<String> strings = delegate.getStrings();
        while (it.hasNext()) {
            Application app = it.next();
            ApplicationState state = service.getState(app.id());
//            if (previousApps.contains(app.id().name())) {
//                continue;
//            }
            if (state == INSTALLED) {
                strings.add(app.id().name());
            }
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }
}