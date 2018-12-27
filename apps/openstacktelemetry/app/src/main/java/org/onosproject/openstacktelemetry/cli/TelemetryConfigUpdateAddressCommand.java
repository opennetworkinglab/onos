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

import com.google.common.collect.Maps;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacktelemetry.api.TelemetryConfigAdminService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.Map;

/**
 * Update telemetry configuration.
 */
@Service
@Command(scope = "onos", name = "telemetry-update-address",
        description = "Update a telemetry address")
public class TelemetryConfigUpdateAddressCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "config name", description = "telemetry config name",
            required = true, multiValued = false)
    @Completion(TelemetryConfigNameCompleter.class)
    private String configName = null;

    @Argument(index = 1, name = "address", description = "telemetry config address",
            required = true, multiValued = false)
    private String address = null;

    private static final String ADDRESS = "address";
    private static final String NO_ELEMENT =
            "No telemetry config is found with the given name";

    @Override
    protected void doExecute() {
        TelemetryConfigAdminService service = get(TelemetryConfigAdminService.class);
        TelemetryConfig config = service.getConfig(configName);

        if (config == null) {
            print(NO_ELEMENT);
            return;
        }

        Map<String, String> updatedProperties = Maps.newHashMap(config.properties());
        updatedProperties.put(ADDRESS, address);

        TelemetryConfig updatedConfig = config.updateProperties(updatedProperties);

        service.updateTelemetryConfig(updatedConfig);
        print("Successfully updated telemetry config address!");
    }
}
