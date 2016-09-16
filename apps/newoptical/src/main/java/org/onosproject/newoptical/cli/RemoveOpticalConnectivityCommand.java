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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.newoptical.api.OpticalConnectivityId;
import org.onosproject.newoptical.api.OpticalPathService;

@Command(scope = "onos", name = "remove-optical-connectivity",
        description = "Remove optical domain connectivity")
public class RemoveOpticalConnectivityCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "id", description = "ID of optical connectivity",
            required = true, multiValued = false)
    String idStr = null;

    @Override
    protected void execute() {
        OpticalPathService opticalPathService = get(OpticalPathService.class);

        OpticalConnectivityId id = OpticalConnectivityId.of(Long.valueOf(idStr));

        print("Trying to remove connectivity with id %s.", idStr);
        if (opticalPathService.removeConnectivity(id)) {
            print(" -- success");
        } else {
            print(" -- failed");
        }

    }
}
