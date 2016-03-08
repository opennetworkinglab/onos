package org.onosproject.cli.net;

import java.util.List;

//import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.flowobjective.FlowObjectiveService;

/**
 * Returns a mapping of FlowObjective next-ids to the groups that get created
 * by a device driver.
 */
@Command(scope = "onos", name = "next-ids",
        description = "flow-objective next-ids to group-ids mapping")
public class FlowObjectiveNextListCommand extends AbstractShellCommand {

    /*@Argument(index = 1, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    String uri = null;
     */
    private static final String FORMAT_MAPPING =
            "  %s";
    @Override
    protected void execute() {
        FlowObjectiveService service = get(FlowObjectiveService.class);
        printNexts(service.getNextMappings());
    }

    private void printNexts(List<String> nextGroupMappings) {
        nextGroupMappings.forEach(str -> print(FORMAT_MAPPING, str));
    }
}
