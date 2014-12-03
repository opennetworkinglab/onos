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

import static org.onosproject.foo.SimpleNettyClient.startStandalone;
import static org.onosproject.foo.SimpleNettyClient.stop;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;

/**
 * Test Netty client performance.
 */
@Command(scope = "onos", name = "simple-netty-client",
        description = "Starts simple Netty client")
public class SimpleNettyClientCommand extends AbstractShellCommand {

    //FIXME: replace these arguments with proper ones needed for the test.
    @Argument(index = 0, name = "hostname", description = "Server Hostname",
            required = false, multiValued = false)
    String hostname = "localhost";

    @Argument(index = 1, name = "port", description = "Port",
            required = false, multiValued = false)
    String port = "8081";

    @Argument(index = 2, name = "warmupCount", description = "Warm-up count",
            required = false, multiValued = false)
    String warmupCount = "1000";

    @Argument(index = 3, name = "messageCount", description = "Message count",
            required = false, multiValued = false)
    String messageCount = "1000000";

    @Override
    protected void execute() {
        try {
            startStandalone(new String[]{hostname, port, warmupCount, messageCount});
        } catch (Exception e) {
            error("Unable to start client %s", e);
        }
        stop();
    }
}
