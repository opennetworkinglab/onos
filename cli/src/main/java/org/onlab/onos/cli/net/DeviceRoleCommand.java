package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.cluster.MastershipAdminService;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.MastershipRole;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Sets role of the controller node for the given infrastructure device.
 */
@Command(scope = "onos", name = "device-role",
         description = "Sets role of the controller node for the given infrastructure device")
public class DeviceRoleCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = true, multiValued = false)
    String uri = null;

    @Argument(index = 1, name = "node", description = "Node ID",
              required = true, multiValued = false)
    String node = null;

    @Argument(index = 2, name = "role", description = "Mastership role",
              required = true, multiValued = false)
    String role = null;

    @Override
    protected void execute() {
        MastershipAdminService service = get(MastershipAdminService.class);
        MastershipRole mastershipRole = MastershipRole.valueOf(role.toUpperCase());
        service.setRole(new NodeId(node), deviceId(uri), mastershipRole);
    }

}
