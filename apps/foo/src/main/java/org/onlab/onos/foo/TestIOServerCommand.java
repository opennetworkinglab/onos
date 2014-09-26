package org.onlab.onos.foo;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;

import static org.onlab.onos.foo.IOLoopTestServer.startStandalone;


/**
 * Starts the test IO loop server.
 */
@Command(scope = "onos", name = "test-io-server",
         description = "Starts the test IO loop server")
public class TestIOServerCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        try {
            startStandalone(new String[]{});
        } catch (Exception e) {
            error("Unable to start server %s", e);
        }
    }

}
