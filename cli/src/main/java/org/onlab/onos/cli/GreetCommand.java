package org.onlab.onos.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.GreetService;

/**
 * Simple command example to demonstrate use of Karaf shell extensions; shows
 * use of an optional parameter as well.
 */
@Command(scope = "onos", name = "greet", description = "Issues a greeting")
public class GreetCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name", description = "Name to greet",
              required = false, multiValued = false)
    String name = "dude";

    @Override
    protected Object doExecute() throws Exception {
        print(getService(GreetService.class).yo(name));
        return null;
    }
}
