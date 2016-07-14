/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.pim.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.pim.impl.PimInterface;
import org.onosproject.pim.impl.PimInterfaceService;

import java.util.Set;

/**
 * Lists the interfaces where PIM is enabled.
 */
@Command(scope = "onos", name = "pim-interfaces",
        description = "Lists the interfaces where PIM is enabled")
public class PimInterfacesListCommand extends AbstractShellCommand {

    private static final String FORMAT = "interfaceName=%s, holdTime=%s, priority=%s, genId=%s";
    private static final String ROUTE_FORMAT = "    %s";

    @Override
    protected void execute() {
        PimInterfaceService interfaceService = get(PimInterfaceService.class);

        Set<PimInterface> interfaces = interfaceService.getPimInterfaces();

        interfaces.forEach(pimIntf -> {
            print(FORMAT, pimIntf.getInterface().name(),
                    pimIntf.getHoldtime(), pimIntf.getPriority(),
                    pimIntf.getGenerationId());

            pimIntf.getRoutes().forEach(route -> print(ROUTE_FORMAT, route));
        });
    }

}
