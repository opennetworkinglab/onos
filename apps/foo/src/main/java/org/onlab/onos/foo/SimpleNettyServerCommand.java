package org.onlab.onos.foo;

import static org.onlab.onos.foo.SimpleNettyServer.startStandalone;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;

/**
 * Starts the Simple Netty server.
 */
@Command(scope = "onos", name = "simple-netty-server",
         description = "Starts the simple netty server")
public class SimpleNettyServerCommand extends AbstractShellCommand {

    //FIXME: Replace these with parameters for
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
