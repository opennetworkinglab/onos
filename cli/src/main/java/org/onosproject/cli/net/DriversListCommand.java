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
package org.onosproject.cli.net;

import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdminService;

import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * Lists device drivers.
 */
@Command(scope = "onos", name = "drivers",
        description = "Lists device drivers")
public class DriversListCommand extends AbstractShellCommand {

    private static final String FMT = "driver=%s, extends=%s, mfr=%s, hw=%s, sw=%s";
    private static final String FMT_B = "   %s via %s";
    private static final String FMT_P = "   %s=%s";

    @Argument(index = 0, name = "driverName", description = "Driver name",
            required = false, multiValued = false)
    String driverName = null;

    @Override
    protected void execute() {
        DriverAdminService service = get(DriverAdminService.class);

        if (driverName != null) {
            printDriver(service.getDriver(driverName));
        } else {
            if (outputJson()) {
                json(service.getDrivers());
            } else {
                service.getDrivers().forEach(this::printDriver);
            }
        }
    }

    private void json(Driver driver) {
        print("%s", jsonForEntity(driver, Driver.class));
    }

    private void json(Set<Driver> drivers) {
        ArrayNode result = mapper().createArrayNode();
        drivers.forEach(driver -> result.add(jsonForEntity(driver, Driver.class)));
        print("%s", result.toString());
    }

    private void printDriver(Driver driver) {
        if (outputJson()) {
            json(driver);
        } else {
            Driver parent = driver.parent();
            print(FMT, driver.name(), parent != null ? parent.name() : "none",
                    driver.manufacturer(), driver.hwVersion(), driver.swVersion());
            driver.behaviours().forEach(b -> print(FMT_B, b.getCanonicalName(),
                    driver.implementation(b).getCanonicalName()));
            driver.properties().forEach((k, v) -> print(FMT_P, k, v));
        }
    }

}
