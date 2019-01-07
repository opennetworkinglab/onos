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

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lists Telemetry configurations.
 */
@Service
@Command(scope = "onos", name = "telemetry-configs",
        description = "Lists all Telemetry configurations")
public class TelemetryConfigListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-30s%-15s%-15s%-30s%-15s";
    private static final String MASTER = "master";

    @Override
    protected void doExecute() {
        TelemetryConfigService service = get(TelemetryConfigService.class);
        List<TelemetryConfig> configs = service.getConfigs().stream()
                .filter(c -> !c.swVersion().equals(MASTER))
                .sorted(Comparator.comparing(TelemetryConfig::type))
                .collect(Collectors.toList());

        print(FORMAT, "Name", "Type", "Enabled", "Manufacturer", "swVersion");
        for (TelemetryConfig config : configs) {
            print(FORMAT, config.name(),
                    config.type(),
                    config.status().name(),
                    config.manufacturer(),
                    config.swVersion());
        }
    }
}
