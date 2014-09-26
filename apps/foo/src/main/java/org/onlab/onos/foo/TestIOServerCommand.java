package org.onlab.onos.foo;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;

import static org.onlab.onos.foo.IOLoopTestServer.startStandalone;

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
