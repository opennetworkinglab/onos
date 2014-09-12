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
    private static final String COMPACT = "%s/%s-%s/%s";

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = false, multiValued = false)
    String uri = null;

    @Override
    protected Object doExecute() throws Exception {
        LinkService service = getService(LinkService.class);
        Iterable<Link> links = uri != null ?
                service.getDeviceLinks(deviceId(uri)) : service.getLinks();
        for (Link link : links) {
            print(linkString(link));
        }
        return null;
    }

    /**
     * Returns a formatted string representing the given link.
     *
     * @param link infrastructure link
     * @return formatted link string
     */
    public static String linkString(Link link) {
        return String.format(FMT, link.src().deviceId(), link.src().port(),
                             link.dst().deviceId(), link.dst().port(), link.type());

    }

    /**
     * Returns a compact string representing the given link.
     *
     * @param link infrastructure link
     * @return formatted link string
     */
    public static String compactLinkString(Link link) {
        return String.format(COMPACT, link.src().deviceId(), link.src().port(),
                             link.dst().deviceId(), link.dst().port());
    }

}
