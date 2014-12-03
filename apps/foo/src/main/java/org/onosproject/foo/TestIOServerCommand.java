/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.foo;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

import static org.onosproject.foo.IOLoopTestServer.startStandalone;

/**
 * Starts the test IO loop server.
 */
@Command(scope = "onos", name = "test-io-server",
         description = "Starts the test IO loop server")
public class TestIOServerCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "serverIp", description = "Server IP address",
              required = false, multiValued = false)
    String serverIp = "127.0.0.1";

    @Argument(index = 1, name = "workers", description = "IO workers",
              required = false, multiValued = false)
    String workers = "6";

    @Argument(index = 2, name = "messageLength", description = "Message length (bytes)",
              required = false, multiValued = false)
    String messageLength = "128";

    @Override
    protected void execute() {
        try {
            startStandalone(new String[]{serverIp, workers, messageLength});
        } catch (Exception e) {
            error("Unable to start server %s", e);
        }
    }

}
