package org.onosproject.segmentrouting.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.config.NetworkConfig.SwitchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Manages additional configuration for switches configured as Segment Routers.
 */
public class SegmentRouterConfig extends SwitchConfig {
    protected static final Logger log = LoggerFactory
            .getLogger(SegmentRouterConfig.class);
    private String routerIp;
    private String routerMac;
    private int nodeSid;
    private boolean isEdgeRouter;
    private List<AdjacencySid> adjacencySids;
    private List<Subnet> subnets;

    public static final String ROUTER_IP = "routerIp";
    public static final String ROUTER_MAC = "routerMac";
    public static final String NODE_SID = "nodeSid";
    public static final String ADJACENCY_SIDS = "adjacencySids";
    public static final String SUBNETS = "subnets";
    public static final String ISEDGE = "isEdgeRouter";
    private static final int SRGB_MAX = 1000;

    /**
     * Parses and validates the additional configuration parameters applicable
     * to segment routers.
     *
     * @param swc switch configuration
     */
    public SegmentRouterConfig(SwitchConfig swc) {
        this.setName(swc.getName());
        this.setDpid(swc.getDpid());
        this.setType(swc.getType());
        this.setLatitude(swc.getLatitude());
        this.setLongitude(swc.getLongitude());
        this.setParams(swc.getParams());
        this.setAllowed(swc.isAllowed());
        publishAttributes = new ConcurrentHashMap<String, String>();
        adjacencySids = new ArrayList<AdjacencySid>();
        subnets = new ArrayList<Subnet>();
        parseParams();
        validateParams();
        setPublishAttributes();
    }

    /**
     * Returns the configured segment router IP address.
     *
     * @return ip address in string format
     */
    public String getRouterIp() {
        return routerIp;
    }

    public void setRouterIp(String routerIp) {
        this.routerIp = routerIp;
    }

    /**
     * Returns the configured segment router mac address.
     *
     * @return mac address in string format
     */
    public String getRouterMac() {
        return routerMac;
    }

    public void setRouterMac(String routerMac) {
        this.routerMac = routerMac;
    }

    /**
     * Returns the configured sID for a segment router.
     *
     * @return segment identifier
     */
    public int getNodeSid() {
        return nodeSid;
    }

    public void setNodeSid(int nodeSid) {
        this.nodeSid = nodeSid;
    }

    /**
     * Returns the flag that indicates the configured segment router
     * is edge or backbone router.
     *
     * @return boolean
     */
    public boolean isEdgeRouter() {
        return isEdgeRouter;
    }

    public void setIsEdgeRouter(boolean isEdge) {
        this.isEdgeRouter = isEdge;
    }

    /**
     * Class representing segment router adjacency identifier.
     */
    public static class AdjacencySid {
        private int adjSid;
        private List<Integer> ports;

        public AdjacencySid(int adjSid, List<Integer> ports) {
            this.ports = ports;
            this.adjSid = adjSid;
        }

        /**
         * Returns the list of ports part of a segment
         * router adjacency identifier.
         *
         * @return list of integers
         */
        public List<Integer> getPorts() {
            return ports;
        }

        public void setPorts(List<Integer> ports) {
            this.ports = ports;
        }

        /**
         * Returns the configured adjacency id of a segment router.
         *
         * @return integer
         */
        public int getAdjSid() {
            return adjSid;
        }

        public void setAdjSid(int adjSid) {
            this.adjSid = adjSid;
        }
    }

    /**
     * Returns the configured adjacent segment IDs for a segment router.
     *
     * @return list of adjacency identifier
     */
    public List<AdjacencySid> getAdjacencySids() {
        return adjacencySids;
    }

    public void setAdjacencySids(List<AdjacencySid> adjacencySids) {
        this.adjacencySids = adjacencySids;
    }

    /**
     * Class representing a subnet attached to a segment router.
     */
    public static class Subnet {
        private int portNo;
        private String subnetIp;

        public Subnet(int portNo, String subnetIp) {
            this.portNo = portNo;
            this.subnetIp = subnetIp;
        }

        /**
         * Returns the port number of segment router on
         * which subnet is attached.
         *
         * @return integer
         */
        public int getPortNo() {
            return portNo;
        }

        public void setPortNo(int portNo) {
            this.portNo = portNo;
        }

        /**
         * Returns the configured subnet address.
         *
         * @return subnet ip address in string format
         */
        public String getSubnetIp() {
            return subnetIp;
        }

        public void setSubnetIp(String subnetIp) {
            this.subnetIp = subnetIp;
        }
    }

    /**
     * Returns the configured subnets for a segment router.
     *
     * @return list of subnets
     */
    public List<Subnet> getSubnets() {
        return subnets;
    }

    public void setSubnets(List<Subnet> subnets) {
        this.subnets = subnets;
    }

    // ********************
    // Helper methods
    // ********************

    private void parseParams() {
        if (params == null) {
            throw new NetworkConfigException.ParamsNotSpecified(name);
        }

        Set<Entry<String, JsonNode>> m = params.entrySet();
        for (Entry<String, JsonNode> e : m) {
            String key = e.getKey();
            JsonNode j = e.getValue();
            if (key.equals("routerIp")) {
                setRouterIp(j.asText());
            } else if (key.equals("routerMac")) {
                setRouterMac(j.asText());
            } else if (key.equals("nodeSid")) {
                setNodeSid(j.asInt());
            } else if (key.equals("isEdgeRouter")) {
                setIsEdgeRouter(j.asBoolean());
            } else if (key.equals("adjacencySids") || key.equals("subnets")) {
                getInnerParams(j, key);
            } else {
                throw new UnknownSegmentRouterConfig(key, dpid);
            }
        }
    }

    private void getInnerParams(JsonNode j, String innerParam) {
        Iterator<JsonNode> innerList = j.elements();
        while (innerList.hasNext()) {
            Iterator<Entry<String, JsonNode>> f = innerList.next().fields();
            int portNo = -1;
            int adjSid = -1;
            String subnetIp = null;
            List<Integer> ports = null;
            while (f.hasNext()) {
                Entry<String, JsonNode> fe = f.next();
                if (fe.getKey().equals("portNo")) {
                    portNo = fe.getValue().asInt();
                } else if (fe.getKey().equals("adjSid")) {
                    adjSid = fe.getValue().asInt();
                } else if (fe.getKey().equals("subnetIp")) {
                    subnetIp = fe.getValue().asText();
                } else if (fe.getKey().equals("ports")) {
                    if (fe.getValue().isArray()) {
                        Iterator<JsonNode> i = fe.getValue().elements();
                        ports = new ArrayList<Integer>();
                        while (i.hasNext()) {
                            ports.add(i.next().asInt());
                        }
                    }
                } else {
                    throw new UnknownSegmentRouterConfig(fe.getKey(), dpid);
                }
            }
            if (innerParam.equals("adjacencySids")) {
                AdjacencySid ads = new AdjacencySid(adjSid, ports);
                adjacencySids.add(ads);
            } else {
                Subnet sip = new Subnet(portNo, subnetIp);
                subnets.add(sip);
            }
        }
    }

    private void validateParams() {
        if (routerIp == null) {
            throw new IpNotSpecified(dpid);
        }
        if (routerMac == null) {
            throw new MacNotSpecified(dpid);
        }
        if (isEdgeRouter && subnets.isEmpty()) {
            throw new SubnetNotSpecifiedInEdgeRouter(dpid);
        }
        if (!isEdgeRouter && !subnets.isEmpty()) {
            throw new SubnetSpecifiedInBackboneRouter(dpid);
        }
        if (nodeSid > SRGB_MAX) {
            throw new NodeLabelNotInSRGB(nodeSid, dpid);
        }
        for (AdjacencySid as : adjacencySids) {
            int label = as.getAdjSid();
            List<Integer> plist = as.getPorts();
            if (label <= SRGB_MAX) {
                throw new AdjacencyLabelInSRGB(label, dpid);
            }
            if (plist.size() <= 1) {
                throw new AdjacencyLabelNotEnoughPorts(label, dpid);
            }
        }


        // TODO more validations
    }

    /**
     * Setting publishAttributes implies that this is the configuration that
     * will be added to Topology.Switch object before it is published on the
     * channel to other controller instances.
     */
    private void setPublishAttributes() {
        publishAttributes.put(ROUTER_IP, routerIp);
        publishAttributes.put(ROUTER_MAC, routerMac);
        publishAttributes.put(NODE_SID, String.valueOf(nodeSid));
        publishAttributes.put(ISEDGE, String.valueOf(isEdgeRouter));
        ObjectMapper mapper = new ObjectMapper();
        try {
            publishAttributes.put(ADJACENCY_SIDS,
                    mapper.writeValueAsString(adjacencySids));
            publishAttributes.put(SUBNETS,
                    mapper.writeValueAsString(subnets));
        } catch (JsonProcessingException e) {
            log.error("Error while writing SR config: {}", e.getCause());
        } catch (IOException e) {
            log.error("Error while writing SR config: {}", e.getCause());
        }
    }

    // ********************
    // Exceptions
    // ********************

    public static class IpNotSpecified extends RuntimeException {
        private static final long serialVersionUID = -3001502553646331686L;

        public IpNotSpecified(DeviceId dpid) {
            super();
            log.error("Router IP address not specified for SR config dpid:{}",
                    dpid);
        }
    }

    public static class MacNotSpecified extends RuntimeException {
        private static final long serialVersionUID = -5850132094884129179L;

        public MacNotSpecified(DeviceId dpid) {
            super();
            log.error("Router Mac address not specified for SR config dpid:{}",
                    dpid);
        }
    }

    public static class UnknownSegmentRouterConfig extends RuntimeException {
        private static final long serialVersionUID = -5750132094884129179L;

        public UnknownSegmentRouterConfig(String key, DeviceId dpid) {
            super();
            log.error("Unknown Segment Router config {} in dpid: {}", key,
                    dpid);
        }
    }

    public static class SubnetNotSpecifiedInEdgeRouter extends RuntimeException {
        private static final long serialVersionUID = -5855458472668581268L;

        public SubnetNotSpecifiedInEdgeRouter(DeviceId dpid) {
            super();
            log.error("Subnet was not specified for edge router in dpid: {}",
                    dpid);
        }
    }

    public static class SubnetSpecifiedInBackboneRouter extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public SubnetSpecifiedInBackboneRouter(DeviceId dpid) {
            super();
            log.error("Subnet was specified in backbone router in dpid: {}",
                    dpid);
        }
    }

    public static class NodeLabelNotInSRGB extends RuntimeException {
        private static final long serialVersionUID = -8482670903748519526L;

        public NodeLabelNotInSRGB(int label, DeviceId dpid) {
            super();
            log.error("Node sif {} specified in not in global label-base "
                    + "in dpid: {}", label,
                    dpid);
        }
    }

    public static class AdjacencyLabelInSRGB extends RuntimeException {
        private static final long serialVersionUID = -8482670903748519526L;

        public AdjacencyLabelInSRGB(int label, DeviceId dpid) {
            super();
            log.error("Adjaceny label {} specified from global label-base "
                    + "in dpid: {}", label,
                    dpid);
        }
    }

    public static class AdjacencyLabelNotEnoughPorts extends RuntimeException {
        private static final long serialVersionUID = -8482670903748519526L;

        public AdjacencyLabelNotEnoughPorts(int label, DeviceId dpid) {
            super();
            log.error("Adjaceny label {} must be specified for at least 2 ports. "
                    + "Adjacency labels for single ports are auto-generated "
                    + "in dpid: {}", label,
                    dpid);
        }
    }
}
