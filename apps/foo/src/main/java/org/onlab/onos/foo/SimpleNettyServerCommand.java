package org.onlab.onos.foo;

import static org.onlab.onos.foo.SimpleNettyServer.startStandalone;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;

/**
 * Starts the Simple Netty server.
 */
@Command(scope = "onos", name = "simple-netty-server",
         description = "Starts simple Netty server")
public class SimpleNettyServerCommand extends AbstractShellCommand {

    //FIXME: Replace these with parameters for
    @Argument(index = 0, name = "port", description = "listen port",
              required = false, multiValued = false)
    String port = "8081";

    @Override
    protected void execute() {
        try {
            startStandalone(new String[]{port});
        } catch (Exception e) {
            error("Unable to start server %s", e);
        }
    }

}
