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
package org.onosproject.cli2.app;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.cli2.AbstractCompleter;
import org.onosproject.core.Application;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.onosproject.app.ApplicationState.ACTIVE;
import static org.onosproject.app.ApplicationState.INSTALLED;
import static org.onosproject.cli2.AbstractShellCommand.get;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application name completer.
 */
@Service
public class ApplicationNameCompleter extends AbstractCompleter {
    private static final Logger log = getLogger(ApplicationNameCompleter.class);

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // Delegate string completer
        StringsCompleter delegate = new StringsCompleter();

        // Command name is the second argument.
        String cmd = commandLine.getArguments()[1];
        log.info("Command is {}", cmd);

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
        int c = 0;
        log.info("Processing apps");
        while (it.hasNext()) {
            Application app = it.next();
            log.info("app #{} is {}", c, app.id().name());
            c++;
            ApplicationState state = service.getState(app.id());
//            if (previousApps.contains(app.id().name())) {
//                continue;
//            }
            if ("uninstall".equals(cmd) || "download".equals(cmd) ||
                    ("activate".equals(cmd) && state == INSTALLED) ||
                    ("deactivate".equals(cmd) && state == ACTIVE)) {
                strings.add(app.id().name());
            }
        }

        // add unique suffix to candidates, if user has something in buffer
        log.info("Command line buffer {} position {}", commandLine.getBuffer(), commandLine.getBufferPosition());
        if (!commandLine.getBuffer().endsWith(cmd)) {
            List<String> suffixCandidates = strings.stream()
                    // remove onos common prefix
                    .map(full -> full.replaceFirst("org\\.onosproject\\.", ""))
                    // a.b.c -> [c, b.c, a.b.c]
                    .flatMap(appName -> {
                        List<String> suffixes = new ArrayList<>();
                        Deque<String> frags = new ArrayDeque<>();
                        // a.b.c -> [c, b, a] -> [c, b.c, a.b.c]
                        Lists.reverse(asList(appName.split("\\."))).forEach(frag -> {
                            frags.addFirst(frag);
                            suffixes.add(frags.stream().collect(Collectors.joining(".")));
                        });
                        return suffixes.stream();
                    })
                    // convert to occurrence map
                    .collect(Collectors.groupingBy(e -> e, Collectors.counting()))
                    .entrySet().stream()
                    // only accept unique suffix
                    .filter(e -> e.getValue() == 1L)
                    .map(Entry::getKey)
                    .collect(Collectors.toList());

            delegate.getStrings().addAll(suffixCandidates);
        }

        // Now let the completer do the work for figuring out what to offer.
        return delegate.complete(session, commandLine, candidates);
    }

}
