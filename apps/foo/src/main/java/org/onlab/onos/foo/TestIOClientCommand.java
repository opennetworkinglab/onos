package org.onlab.onos.foo;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;

import static org.onlab.onos.foo.IOLoopTestClient.startStandalone;

/**
 * Starts the test IO loop client.
 */
@Command(scope = "onos", name = "test-io-client",
         description = "Starts the test IO loop client")
public class TestIOClientCommand extends AbstractShellCommand {

    @Override
    protected void execute() {
        try {
            startStandalone(new String[]{});
        } catch (Exception e) {
            error("Unable to start server %s", e);
        }
    }

}
