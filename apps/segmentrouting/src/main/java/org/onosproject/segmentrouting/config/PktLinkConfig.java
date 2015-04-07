package org.onosproject.segmentrouting.config;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.onosproject.net.Link;
import org.onosproject.segmentrouting.config.NetworkConfig.LinkConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Reserved for future use.
 * Configuration for a link between two packet-switches.
 */
public class PktLinkConfig extends LinkConfig {
    protected static final Logger log = LoggerFactory
            .getLogger(PktLinkConfig.class);
    private int port1;
    private int port2;
    private String nodeName1;
    private String nodeName2;
    private List<Link> linkTupleList;

    public PktLinkConfig(LinkConfig lkc) {
        nodeDpid1 = lkc.getNodeDpid1();
        nodeDpid2 = lkc.getNodeDpid2();
        dpid1 = lkc.getDpid1();
        dpid2 = lkc.getDpid2();
        type = lkc.getType();
        allowed = lkc.isAllowed();
        params = lkc.getParams();
        publishAttributes = new ConcurrentHashMap<String, String>();
        parseParams();
        validateParams();
        setPublishAttributes();
    }

    // ********************
    // Packet Link Configuration
    // ********************

    public int getPort1() {
        return port1;
    }

    public void setPort1(int port1) {
        this.port1 = port1;
    }

    public int getPort2() {
        return port2;
    }

    public void setPort2(int port2) {
        this.port2 = port2;
    }

    public String getNodeName1() {
        return nodeName1;
    }

    public void setNodeName1(String nodeName1) {
        this.nodeName1 = nodeName1;
    }

    public String getNodeName2() {
        return nodeName2;
    }

    public void setNodeName2(String nodeName2) {
        this.nodeName2 = nodeName2;
    }

    /**
     * Returns the two unidirectional links corresponding to the packet-link
     * configuration. It is possible that the ports in the LinkTuple have
     * portnumber '0', implying that the configuration applies to all links
     * between the two switches.
     *
     * @return a list of LinkTuple with exactly 2 unidirectional links
     */
    public List<Link> getLinkTupleList() {
        return linkTupleList;
    }

    private void setPublishAttributes() {

    }

    private void parseParams() {
        if (params == null) {
            throw new PktLinkParamsNotSpecified(nodeDpid1, nodeDpid2);
        }
        Set<Entry<String, JsonNode>> m = params.entrySet();
        for (Entry<String, JsonNode> e : m) {
            String key = e.getKey();
            JsonNode j = e.getValue();
            if (key.equals("nodeName1")) {
                setNodeName1(j.asText());
            } else if (key.equals("nodeName2")) {
                setNodeName2(j.asText());
            } else if (key.equals("port1")) {
                setPort1(j.asInt());
            } else if (key.equals("port2")) {
                setPort2(j.asInt());
            } else {
                throw new UnknownPktLinkConfig(key, nodeDpid1, nodeDpid2);
            }
        }
    }

    private void validateParams() {
        // TODO - wrong-names, duplicate links,
        // duplicate use of port, is switch-allowed for which link is allowed?
        // valid port numbers
    }

    public static class PktLinkParamsNotSpecified extends RuntimeException {
        private static final long serialVersionUID = 6247582323691265513L;

        public PktLinkParamsNotSpecified(String dpidA, String dpidB) {
            super();
            log.error("Params required for packet link - not specified "
                    + "for link between switch1:{} and switch2:{}",
                    dpidA, dpidB);
        }
    }

    public static class UnknownPktLinkConfig extends RuntimeException {
        private static final long serialVersionUID = -5750132094884129179L;

        public UnknownPktLinkConfig(String key, String dpidA, String dpidB) {
            super();
            log.error("Unknown packet-link config {} for link between"
                    + " dpid1: {} and dpid2: {}", key,
                    dpidA, dpidB);
        }
    }

}
