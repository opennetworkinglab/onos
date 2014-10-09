package org.onlab.onos.foo;

import static org.onlab.onos.foo.SimpleNettyClient.startStandalone;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;

/**
 * Test Netty client performance.
 */
@Command(scope = "onos", name = "simple-netty-client",
        description = "Starts the simple Netty client")
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
    String messageCount = "100000";

    @Override
    protected void execute() {
        try {
            startStandalone(new String[]{hostname, port, warmupCount, messageCount});
        } catch (Exception e) {
            error("Unable to start client %s", e);
        }
    }
}
