/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.net.intf.Interface;
import org.onosproject.net.neighbour.NeighbourResolutionService;

/**
 * Lists neighbour message handlers.
 */
@Service
@Command(scope = "onos", name = "neighbour-handlers",
        description = "Lists neighbour message handlers")
public class NeighbourHandlersListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%20s: interface=%s, class=%s";

    @Override
    protected void doExecute() {
        NeighbourResolutionService service = get(NeighbourResolutionService.class);

        service.getHandlerRegistrations().forEach((cp, list) -> {
            list.forEach(hr -> print(FORMAT, cp, intfToName(hr.intf()),
                    hr.handler().getClass().getCanonicalName()));
        });
    }

    private String intfToName(Interface intf) {
        return (intf == null) ?  "(None)" : intf.name();
    }
}
