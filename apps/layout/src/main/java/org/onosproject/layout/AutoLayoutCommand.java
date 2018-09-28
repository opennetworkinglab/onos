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
package org.onosproject.layout;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Lays out the elements in the topology using the specified algorithm.
 */
@Service
@Command(scope = "onos", name = "topo-layout",
        description = "Lays out the elements in the topology using the specified algorithm")
public class AutoLayoutCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "algorithm",
            description = "Layout algorithm to use for the layout; defaults to 'access'")
    String algorithm = "access";

    @Override
    protected void doExecute() {
        RoleBasedLayoutManager mgr = get(RoleBasedLayoutManager.class);
        switch (algorithm) {
            case "access":
                mgr.layout(new AccessNetworkLayout());
                break;
            case "default":
                mgr.layout(new DefaultForceLayout());
                break;
            default:
                print("Unsupported layout algorithm %s", algorithm);
        }
    }
}
