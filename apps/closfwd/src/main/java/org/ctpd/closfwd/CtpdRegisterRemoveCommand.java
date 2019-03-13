package org.ctpd.closfwd;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Iterator;


import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.HostId;

import org.ctpd.closfwd.ClosDeviceService;
import org.ctpd.closfwd.Endpoint;

import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.XmlString;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.cli.net.PortNumberCompleter;


@Service
@Command(scope = "onos", name = "clos-remove-registry",
        description = "Remove a registry with a UUID")


public class CtpdRegisterRemoveCommand extends AbstractShellCommand {

    // the String to hold the optional argument
    @Argument(index = 0, name = "uuid", description = "UUID of the requested entry. If not provided, You can´t remove a registry.",
            required = false, multiValued = false)
    private String uuid = null;


    // reference to our service
    private ClosDeviceService service;


    @Override
    protected void doExecute() {
        service = get(ClosDeviceService.class);
        if(uuid==null)
        {
            print("No valid command, choose a UUID to remove");

        }
        else
        {
            Endpoint device = service.removeEndpoint(UUID.fromString(uuid));

            if(device!=null)
            {
                print("Remove Registry: "+uuid);

            }
            else
            {
                print("No device found for: "+uuid);
            }

}
    }
}




