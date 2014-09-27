package org.onlab.onos.foo;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;

import static org.onlab.onos.foo.IOLoopTestClient.startStandalone;

/**
 * Starts the test IO loop client.
 */
@Command(scope = "onos", name = "test-io-client",
         description = "Starts the test IO loop client")
public class TestIOClientCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "serverIp", description = "Server IP address",
              required = false, multiValued = false)
    String serverIp = "127.0.0.1";

    @Argument(index = 1, name = "workers", description = "IO workers",
              required = false, multiValued = false)
    String workers = "6";

    @Argument(index = 2, name = "messageCount", description = "Message count",
              required = false, multiValued = false)
    String messageCount = "1000000";

    @Argument(index = 3, name = "messageLength", description = "Message length (bytes)",
              required = false, multiValued = false)
    String messageLength = "128";

    @Argument(index = 4, name = "timeoutSecs", description = "Test timeout (seconds)",
              required = false, multiValued = false)
    String timeoutSecs = "60";

    @Override
    protected void execute() {
        try {
            startStandalone(new String[]{serverIp, workers, messageCount, messageLength, timeoutSecs});
        } catch (Exception e) {
            error("Unable to start client %s", e);
        }
    }

}
