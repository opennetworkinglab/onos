package org.onosproject.drivers.cisco;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.PortStatsQuery;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieve port stats from Cisco CSR1000v router via netconf.
 */
public class PortStatsQueryCsr1000vImpl extends AbstractHandlerBehaviour
        implements PortStatsQuery {

    private final Logger log = getLogger(getClass());

    @Override
    public Collection<PortStatistics> getPortStatistics(DeviceId deviceId) {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();
        String reply;
        try {
            reply = session.get(getPortStatsRequestBuilder());
            /* Cisco use '\r\n' as newline character, to correctly log on Linux, we need to remove '\r' */
            log.debug("Device {} replies {}", deviceId, reply.replaceAll("\r", ""));
        } catch (IOException e) {
            throw new RuntimeException(new NetconfException("Failed to retrieve port stats info", e));
        }

        Collection<PortStatistics> ps = parseCsr1000vPortStats(XmlConfigParser.
                loadXml(new ByteArrayInputStream(reply.getBytes())));
        return ps;
    }

    /**
     * Builds a request crafted to get the configuration required to create port
     * descriptions for the device.
     * @return The request string.
     */
    private String getPortStatsRequestBuilder() {
        StringBuilder rpc = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        //Message ID is injected later.
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter>");
        rpc.append("<config-format-text-cmd>");
        /* Use regular expression to extract only name of the interfaces from running-config */
        rpc.append("<text-filter-spec> | include interface </text-filter-spec>");
        rpc.append("</config-format-text-cmd>");
        rpc.append("<oper-data-format-text-block>");
        /* Use regular expression to extract status of the interfaces
         * Note that there is an space after 'packet'
         */
        rpc.append("<exec>show interfaces | include (packets )</exec>");
        rpc.append("</oper-data-format-text-block>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");

        return rpc.toString();
    }

    /**
     * Parses a configuration and returns a set port stats for Cisco CSR1000v.
     * @param cfg a hierarchical configuration but might not in pure XML format
     * @return a list of port stats
     */
    private Collection<PortStatistics> parseCsr1000vPortStats(HierarchicalConfiguration cfg) {
        List<PortStatistics> ps = Lists.newArrayList();
        List<Object> portNames = cfg.getList("data.cli-config-data.cmd");
        List<Object> portStatsInfo = cfg.getList("data.cli-oper-data-block.item.response");
        int numberOfPorts = portNames.size();
        if (portStatsInfo.size() != numberOfPorts * 4 + 1) {
            log.error("Failed to match portStatus against portName");
            return ps;
        }
        for (int i = 0; i < numberOfPorts; i++) {
            /*  Interface port numbering is from 1 on CSR1000v and up to the number of interfaces supported */
            /* Example string:
             *        161683 packets input, 10929688 bytes, 0 no buffer <-- port 1
             *        126885 packets output, 9412245 bytes, 0 underruns
             *        8 packets input, 648 bytes, 0 no buffer           <-- port 2
             *        213 packets output, 16349 bytes, 0 underruns
             */

            ps.add(DefaultPortStatistics.builder()
                            .setPort(i + 1) /*  Interface port numbering is from 1 on CSR1000v */
                            .setPacketsReceived(getNumber(portStatsInfo.get(i * 4).toString()))
                            .setBytesReceived(getNumber(portStatsInfo.get(i * 4 + 1).toString()))
                            .setPacketsSent(getNumber(portStatsInfo.get(i * 4 + 2).toString()))
                            .setBytesSent(getNumber(portStatsInfo.get(i * 4 + 3).toString()))
                            .build());
        }

        return ps;
    }

    private static long getNumber(String str) {
        /* Deal with "123 no buffer 123456 packets output" */
        str = str.replaceAll("[0-9]+ no buffer", "");
        /* Deal with "123 underruns 123456 packets input" */
        str = str.replaceAll("[0-9]+ underruns", "");
        /* Extract numbers only */
        str = str.replaceAll("[^0-9]", "");

        return Long.parseLong(str);
    }
}
