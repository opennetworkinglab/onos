/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cli;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

import java.util.Formatter;

/**
 * CLI command to dump out the SCR components list in the old karaf format.
 */
@Service
@Command(scope = "onos", name = "scr-list",
         description = "List components")
public class ScrListCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        StringBuilder output = new StringBuilder();
        Formatter formatter = new Formatter(output);
        ServiceComponentRuntime scrService = get(ServiceComponentRuntime.class);

        scrService.getComponentDescriptionDTOs()
            .forEach(componentDto -> {
                scrService.getComponentConfigurationDTOs(componentDto)
                    .forEach(configurationDto -> {
                        String state;
                        switch (configurationDto.state) {
                            case ComponentConfigurationDTO.ACTIVE: {
                                state = "ACTIVE";
                                break;
                            }
                            case ComponentConfigurationDTO.SATISFIED: {
                                state = "SATISFIED";
                                break;
                            }
                            default: {
                                state = "UNKNOWN";
                            }
                        }
                        formatter.format("%3d | %9s | %s\n", configurationDto.id, state, componentDto.name);
                    });
                });
        print(output.toString());
    }

}
