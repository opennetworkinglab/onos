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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Queries the detailed information of telemetry config.
 */
@Service
@Command(scope = "onos", name = "telemetry-config",
        description = "Query a specific telemetry configuration")
public class TelemetryConfigViewCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "config name", description = "telemetry config name",
            required = true, multiValued = false)
    @Completion(TelemetryConfigNameCompleter.class)
    private String configName = null;

    private static final String FORMAT = "%25s : %s";
    private static final String NO_ELEMENT =
            "No telemetry config is found with the given name";

    @Override
    protected void doExecute() {
        TelemetryConfigService service = get(TelemetryConfigService.class);
        TelemetryConfig config = service.getConfig(configName);

        if (config == null) {
            print(NO_ELEMENT);
            return;
        }

        SortedSet<String> keys = new TreeSet<>(config.properties().keySet());

        keys.forEach(k -> {
            print(FORMAT, k, config.properties().get(k));
        });
    }
}
