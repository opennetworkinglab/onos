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
package org.onosproject.openstacktelemetry.cli;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;

import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Telemetry configuration property completer.
 */
@Service
public class TelemetryConfigNameCompleter implements Completer {

    private static final String MASTER = "master";

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        TelemetryConfigService service = get(TelemetryConfigService.class);

        Set<String> set = service.getConfigs().stream()
                .filter(c -> !c.swVersion().equals(MASTER))
                .sorted(Comparator.comparing(TelemetryConfig::name))
                .map(TelemetryConfig::name)
                .collect(Collectors.toSet());

        SortedSet<String> strings = delegate.getStrings();
        strings.addAll(set);
        return delegate.complete(session, commandLine, candidates);
    }
}
