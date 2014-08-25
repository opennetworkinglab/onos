package org.onlab.onos.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.onlab.onos.net.GreetService;

/**
 * Simple command example to demonstrate use of Karaf shell extensions; shows
 * use of an optional parameter as well.
 */
@Command(scope = "onos", name = "greet", description = "Issues a greeting")
public class GreetCommand extends OsgiCommandSupport {

    @Argument(index = 0, name = "name", description = "Name to greet",
              required = false, multiValued = false)
    String name = "dude";

    @Override
    protected Object doExecute() throws Exception {
        System.out.println(getService(GreetService.class).yo(name));
        return null;
    }
}
