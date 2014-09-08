package org.onlab.onos.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.link.LinkService;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Lists all infrastructure links.
 */
@Command(scope = "onos", name = "links",
         description = "Lists all infrastructure links")
public class LinksListCommand extends AbstractShellCommand {

    private static final String FMT = "src=%s/%s, dst=%s/%s, type=%s";

    @Argument(index = 0, name = "deviceId", description = "Device ID",
              required = false, multiValued = false)
    String deviceId = null;


    @Override
    protected Object doExecute() throws Exception {
        LinkService service = getService(LinkService.class);
        Iterable<Link> links = deviceId != null ?
                service.getDeviceLinks(deviceId(deviceId)) : service.getLinks();
        for (Link link : links) {
            print(FMT, link.src().deviceId(), link.src().port(),
                  link.dst().deviceId(), link.dst().port(), link.type());
        }
        return null;
    }
}
