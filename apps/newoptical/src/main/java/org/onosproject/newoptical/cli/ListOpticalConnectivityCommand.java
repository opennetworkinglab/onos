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
package org.onosproject.newoptical.cli;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.LinkKey;
import org.onosproject.newoptical.OpticalConnectivity;
import org.onosproject.newoptical.api.OpticalPathService;

@Command(scope = "onos", name = "list-optical-connectivity",
        description = "List optical domain connectivity")
public class ListOpticalConnectivityCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        OpticalPathService opticalPathService = get(OpticalPathService.class);

        Collection<OpticalConnectivity> connectivities = opticalPathService.listConnectivity();

        for (OpticalConnectivity connectivity : connectivities) {
            print("Optical connectivity ID: %s", connectivity.id().id());
            print(" links: %s",
                                connectivity.links().stream()
                                    .map(LinkKey::linkKey)
                                    .map(lk -> lk.src() + "-" + lk.dst())
                                    .collect(Collectors.joining(", ")));
            print(" Bandwidth: %s, Latency: %s", connectivity.bandwidth(), connectivity.latency());
            print(" Intent Keys: %s",
                  opticalPathService.listIntents(connectivity.id()));

        }
    }
}
