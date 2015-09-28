/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.mfwd.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mfwd.impl.McastRouteTable;

/**
 * Deletes a multicast route.
 */
@Command(scope = "onos", name = "mcast-delete",
        description = "Delete a multicast route flow")
public class McastDeleteCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "sAddr",
            description = "IP Address of the multicast source. '*' can be used for any source (*, G) entry",
            required = true, multiValued = false)
    String sAddr = null;

    @Argument(index = 1, name = "gAddr",
            description = "IP Address of the multicast group",
            required = true, multiValued = false)
    String gAddr = null;

    @Override
    protected void execute() {
        McastRouteTable mrib = McastRouteTable.getInstance();
        mrib.removeRoute(sAddr, gAddr);
    }
}