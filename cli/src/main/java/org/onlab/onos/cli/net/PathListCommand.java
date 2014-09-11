package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.net.Path;

import java.util.Set;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Lists all shortest-paths paths between the specified source and
 * destination devices.
 */
@Command(scope = "onos", name = "paths",
         description = "Lists all shortest-paths paths between the specified source and destination devices")
public class PathListCommand extends TopologyCommand {

    private static final String FMT = "src=%s/%s, dst=%s/%s, type=%s";

    @Argument(index = 0, name = "src", description = "Source device ID",
              required = true, multiValued = false)
    String src = null;

    @Argument(index = 0, name = "dst", description = "Destination device ID",
              required = true, multiValued = false)
    String dst = null;

    @Override
    protected Object doExecute() throws Exception {
        init();
        Set<Path> paths = service.getPaths(topology, deviceId(src), deviceId(dst));
        for (Path path : paths) {
            print(pathString(path));
        }
        return null;
    }

    private String pathString(Path path) {
        return path.toString();
    }

}
