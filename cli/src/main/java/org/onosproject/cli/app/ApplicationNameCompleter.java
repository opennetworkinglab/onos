/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cli.app;

import org.apache.karaf.shell.console.completer.ArgumentCompleter;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.cli.AbstractCompleter;
import org.onosproject.core.Application;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import static org.onosproject.app.ApplicationState.ACTIVE;
import static org.onosproject.app.ApplicationState.INSTALLED;
import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Application name completer.
 */
public class ApplicationNameCompleter extends AbstractCompleter {
    @Override
    public int complete(String buffer, int cursor, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Command name is the second argument.
        ArgumentCompleter.ArgumentList list = getArgumentList();
        String cmd = list.getArguments()[1];

        // Grab apps already on the command (to prevent tab-completed duplicates)
        // FIXME: This does not work.
//        final Set previousApps;
//        if (list.getArguments().length > 2) {
//            previousApps = Sets.newHashSet(
//                    Arrays.copyOfRange(list.getArguments(), 2, list.getArguments().length));
//        } else {
//            previousApps = Collections.emptySet();
//        }

        // Fetch our service and feed it's offerings to the string completer
        ApplicationService service = get(ApplicationService.class);
        Iterator<Application> it = service.getApplications().iterator();
        SortedSet<String> strings = delegate.getStrings();
        while (it.hasNext()) {
            Application app = it.next();
            ApplicationState state = service.getState(app.id());
//            if (previousApps.contains(app.id().name())) {
//                continue;
//            }
            if ("uninstall".equals(cmd) ||
                    ("activate".equals(cmd) && state == INSTALLED) ||
                    ("deactivate".equals(cmd) && state == ACTIVE)) {
                strings.add(app.id().name());
            }
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(buffer, cursor, candidates);
    }

}
