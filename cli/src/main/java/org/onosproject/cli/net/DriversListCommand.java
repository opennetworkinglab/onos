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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
        DriverService service = get(DriverService.class);

        if (driverName != null) {
            printDriver(service.getDriver(driverName), true);
        } else {
            if (outputJson()) {
                json(service.getDrivers());
            } else {
                service.getDrivers().forEach(d -> printDriver(d, true));
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

    private void printDriver(Driver driver, boolean first) {
        if (outputJson()) {
            json(driver);
        } else {

            List<Driver> parents = Optional.ofNullable(driver.parents())
                    .orElse(ImmutableList.of());

            List<String> parentsNames = parents.stream()
                    .map(Driver::name).collect(Collectors.toList());

            if (first) {
                print(FMT, driver.name(), parentsNames,
                      driver.manufacturer(), driver.hwVersion(), driver.swVersion());
            } else {
                print("   Inherited from %s", driver.name());
            }

            driver.behaviours().forEach(b -> printBehaviour(b, driver));
            //recursion call to print each parent
            parents.stream().forEach(parent -> printDriver(parent, false));
            driver.properties().forEach((k, v) -> print(FMT_P, k, v));
        }
    }

    private void printBehaviour(Class<? extends Behaviour> behaviour, Driver driver) {
        print(FMT_B, behaviour.getCanonicalName(),
              driver.implementation(behaviour).getCanonicalName());
    }

}
