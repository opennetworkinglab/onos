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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;
import org.onosproject.net.driver.DriverProvider;

import java.util.stream.Collectors;

/**
 * Lists device drivers.
 */
@Service
@Command(scope = "onos", name = "driver-providers",
        description = "Lists device driver providers")
public class DriverProvidersListCommand extends AbstractShellCommand {

    private static final String FMT = "provider=%s, drivers=%s";

    @Override
    protected void doExecute() {
        DriverAdminService service = get(DriverAdminService.class);
        service.getProviders().forEach(this::printDriverProvider);
    }

    private void printDriverProvider(DriverProvider provider) {
        print(FMT, provider.getClass().getName(),
              provider.getDrivers().stream()
                      .map(Driver::name)
                      .collect(Collectors.toSet()));
    }

}
