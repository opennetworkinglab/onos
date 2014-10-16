package org.onlab.onos.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.ConnectPoint;
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
    protected void execute() {
        LinkService service = get(LinkService.class);
        Iterable<Link> links = uri != null ?
                service.getDeviceLinks(deviceId(uri)) : service.getLinks();
        if (outputJson()) {
            print("%s", json(links));
        } else {
            for (Link link : links) {
                print(linkString(link));
            }
        }
    }

    /**
     * Produces a JSON array containing the specified links.
     *
     * @param links collection of links
     * @return JSON array
     */
    public static JsonNode json(Iterable<Link> links) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Link link : links) {
            result.add(json(mapper, link));
        }
        return result;
    }

    /**
     * Produces a JSON object for the specified link.
     *
     * @param mapper object mapper
     * @param link   link to encode
     * @return JSON object
     */
    public static ObjectNode json(ObjectMapper mapper, Link link) {
        ObjectNode result = mapper.createObjectNode();
        result.set("src", json(mapper, link.src()));
        result.set("dst", json(mapper, link.dst()));
        return result;
    }

    /**
     * Produces a JSON object for the specified connect point.
     *
     * @param mapper       object mapper
     * @param connectPoint connection point to encode
     * @return JSON object
     */
    public static ObjectNode json(ObjectMapper mapper, ConnectPoint connectPoint) {
        return mapper.createObjectNode()
                .put("device", connectPoint.deviceId().toString())
                .put("port", connectPoint.port().toString());
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
