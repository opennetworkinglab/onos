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
import org.onosproject.openstacktelemetry.api.TelemetryConfigAdminService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import static java.lang.Thread.sleep;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.DISABLED;

/**
 * Disables a telemetry service.
 */
@Service
@Command(scope = "onos", name = "telemetry-disable",
        description = "Disable a specific telemetry service")
public class TelemetryServiceDisableCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "config name", description = "telemetry config name",
            required = true, multiValued = false)
    @Completion(TelemetryConfigNameCompleter.class)
    private String configName = null;

    private static final long SLEEP_MS = 2000; // wait 2s for checking status
    private static final String SUCCESS_FORMAT = "Successfully disabled telemetry service %s!";
    private static final String FAIL_FORMAT = "Failed to disable telemetry service %s!";
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

        TelemetryConfig updatedConfig = config.updateStatus(DISABLED);

        service.updateTelemetryConfig(updatedConfig);

        try {
            sleep(SLEEP_MS);
        } catch (InterruptedException e) {
            error("Exception caused during status checking...");
        }

        TelemetryConfig finalConfig = service.getConfig(configName);

        if (finalConfig.status() == DISABLED) {
            print(SUCCESS_FORMAT, config.name());
        } else {
            print(FAIL_FORMAT, config.name());
        }
    }
}
