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

package org.onosproject.cordvtn.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cordvtn.CordVtnService;
import org.onosproject.cordvtn.CordVtnNode;

import java.util.NoSuchElementException;

/**
 * Deletes nodes from the service.
 */
@Command(scope = "onos", name = "cordvtn-node-delete",
        description = "Deletes nodes from CORD VTN service")
public class CordVtnNodeDeleteCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostnames", description = "Hostname(s)",
            required = true, multiValued = true)
    private String[] hostnames = null;

    @Override
    protected void execute() {
        CordVtnService service = AbstractShellCommand.get(CordVtnService.class);

        for (String hostname : hostnames) {
            CordVtnNode node;
            try {
                node = service.getNodes()
                        .stream()
                        .filter(n -> n.hostname().equals(hostname))
                        .findFirst().get();
            } catch (NoSuchElementException e) {
                print("Unable to find %s", hostname);
                continue;
            }

            service.deleteNode(node);
        }
    }
}
