package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;

import java.util.Set;

import static org.onlab.onos.cli.net.LinksListCommand.compactLinkString;
import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Lists all shortest-paths paths between the specified source and
 * destination devices.
 */
@Command(scope = "onos", name = "paths",
         description = "Lists all shortest-paths paths between the specified source and destination devices")
public class PathListCommand extends TopologyCommand {

    private static final String SEP = "==>";

    @Argument(index = 0, name = "src", description = "Source device ID",
              required = true, multiValued = false)
    String src = null;

    @Argument(index = 1, name = "dst", description = "Destination device ID",
              required = true, multiValued = false)
    String dst = null;

    @Override
    protected void execute() {
        init();
        Set<Path> paths = service.getPaths(topology, deviceId(src), deviceId(dst));
        for (Path path : paths) {
            print(pathString(path));
        }
    }

    /**
     * Produces a formatted string representing the specified path.
     *
     * @param path network path
     * @return formatted path string
     */
    protected String pathString(Path path) {
        StringBuilder sb = new StringBuilder();
        for (Link link : path.links()) {
            sb.append(compactLinkString(link)).append(SEP);
        }
        sb.delete(sb.lastIndexOf(SEP), sb.length());
        sb.append("; cost=").append(path.cost());
        return sb.toString();
    }

}
