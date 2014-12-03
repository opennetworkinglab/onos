package org.onosproject.sdnip.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.sdnip.SdnIpService;

/**
 * Command to change whether this SDNIP instance is primary or not.
 */
@Command(scope = "onos", name = "sdnip-set-primary",
         description = "Changes the primary status of this SDN-IP instance")
public class PrimaryChangeCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "isPrimary",
            description = "True if this instance should be primary, false if not",
            required = true, multiValued = false)
    boolean isPrimary = false;

    @Override
    protected void execute() {
        get(SdnIpService.class).modifyPrimary(isPrimary);
    }

}
