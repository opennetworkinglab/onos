package org.onosproject.segmentrouting.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Public class corresponding to JSON described data model. Defines the network
 * configuration at startup.
 */
public class NetworkConfig {
    protected static final Logger log = LoggerFactory.getLogger(NetworkConfig.class);

    @SuppressWarnings("unused")
    private String comment;

    private Boolean restrictSwitches;
    private Boolean restrictLinks;
    private List<SwitchConfig> switches;
    private List<LinkConfig> links;

    /**
     * Default constructor.
     */
    public NetworkConfig() {
        switches = new ArrayList<SwitchConfig>();
        links = new ArrayList<LinkConfig>();
    }

    @JsonProperty("comment")
    public void setComment(String c) {
        log.trace("NetworkConfig: comment={}", c);
        comment = c;
    }

    @JsonProperty("restrictSwitches")
    public void setRestrictSwitches(boolean rs) {
        log.trace("NetworkConfig: restrictSwitches={}", rs);
        restrictSwitches = rs;
    }

    /**
     * Returns default restrict configuration for switches.
     *
     * @return boolean
     */
    public Boolean getRestrictSwitches() {
        return restrictSwitches;
    }

    @JsonProperty("restrictLinks")
    public void setRestrictLinks(boolean rl) {
        log.trace("NetworkConfig: restrictLinks={}", rl);
        restrictLinks = rl;
    }

    /**
     * Returns default restrict configuration for links.
     *
     * @return boolean
     */
    public Boolean getRestrictLinks() {
        return restrictLinks;
    }

    /**
     * Returns configuration for switches.
     *
     * @return list of switch configuration
     */
    public List<SwitchConfig> getSwitchConfig() {
        return switches;
    }

    @JsonProperty("switchConfig")
    public void setSwitchConfig(List<SwitchConfig> switches2) {
        log.trace("NetworkConfig: switchConfig={}", switches2);
        this.switches = switches2;
    }

    /**
     * Java class corresponding to JSON described switch
     * configuration data model.
     */
    public static class SwitchConfig {
        protected String nodeDpid;
        protected String name;
        protected String type;
        protected boolean allowed;
        protected double latitude;
        protected double longitude;
        protected Map<String, JsonNode> params;
        protected Map<String, String> publishAttributes;
        protected DeviceId dpid;

        /**
         * Returns the configured "name" of a switch.
         *
         * @return string
         */
        public String getName() {
            return name;
        }

        @JsonProperty("name")
        public void setName(String name) {
            log.trace("SwitchConfig: name={}", name);
            this.name = name;
        }

        /**
         * Returns the data plane identifier of a switch.
         *
         * @return ONOS device identifier
         */
        public DeviceId getDpid() {
            return dpid;
        }

        public void setDpid(DeviceId dpid) {
            this.dpid = dpid;
            this.nodeDpid = dpid.toString();
        }

        /**
         * Returns the data plane identifier of a switch.
         *
         * @return string
         */
        public String getNodeDpid() {
            return nodeDpid;
        }

        // mapper sets both DeviceId and string fields for dpid
        @JsonProperty("nodeDpid")
        public void setNodeDpid(String nodeDpid) {
            log.trace("SwitchConfig: nodeDpid={}", nodeDpid);
            this.nodeDpid = nodeDpid;
            this.dpid = DeviceId.deviceId(nodeDpid);
        }

        /**
         * Returns the type of a switch.
         *
         * @return string
         */
        public String getType() {
            return type;
        }

        @JsonProperty("type")
        public void setType(String type) {
            log.trace("SwitchConfig: type={}", type);
            this.type = type;
        }

        /**
         * Returns the latitude of a switch.
         *
         * @return double
         */
        public double getLatitude() {
            return latitude;
        }

        @JsonProperty("latitude")
        public void setLatitude(double latitude) {
            log.trace("SwitchConfig: latitude={}", latitude);
            this.latitude = latitude;
        }

        /**
         * Returns the longitude of a switch.
         *
         * @return double
         */
        public double getLongitude() {
            return longitude;
        }

        @JsonProperty("longitude")
        public void setLongitude(double longitude) {
            log.trace("SwitchConfig: longitude={}", longitude);
            this.longitude = longitude;
        }

        /**
         * Returns the allowed flag for a switch.
         *
         * @return boolean
         */
        public boolean isAllowed() {
            return allowed;
        }

        @JsonProperty("allowed")
        public void setAllowed(boolean allowed) {
            this.allowed = allowed;
        }

        /**
         * Returns the additional configured parameters of a switch.
         *
         * @return key value map
         */
        public Map<String, JsonNode> getParams() {
            return params;
        }

        @JsonProperty("params")
        public void setParams(Map<String, JsonNode> params) {
            this.params = params;
        }

        /**
         * Reserved for future use.
         *
         * @return key value map
         */
        public Map<String, String> getPublishAttributes() {
            return publishAttributes;
        }

        @JsonProperty("publishAttributes")
        public void setPublishAttributes(Map<String, String> publishAttributes) {
            this.publishAttributes = publishAttributes;
        }

    }

    @JsonProperty("linkConfig")
    public void setLinkConfig(List<LinkConfig> links2) {
        this.links = links2;
    }

    /**
     * Reserved for future use.
     *
     * @return list of configured link configuration
     */
    public List<LinkConfig> getLinkConfig() {
        return links;
    }

    /**
     * Reserved for future use.
     */
    public static class LinkConfig {
        protected String type;
        protected Boolean allowed;
        protected DeviceId dpid1;
        protected DeviceId dpid2;
        protected String nodeDpid1;
        protected String nodeDpid2;
        protected Map<String, JsonNode> params;
        protected Map<String, String> publishAttributes;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Boolean isAllowed() {
            return allowed;
        }

        public void setAllowed(Boolean allowed) {
            this.allowed = allowed;
        }

        public String getNodeDpid1() {
            return nodeDpid1;
        }

        // mapper sets both long and string fields for dpid
        public void setNodeDpid1(String nodeDpid1) {
            this.nodeDpid1 = nodeDpid1;
            this.dpid1 = DeviceId.deviceId(nodeDpid1);
        }

        public String getNodeDpid2() {
            return nodeDpid2;
        }

        // mapper sets both long and string fields for dpid
        public void setNodeDpid2(String nodeDpid2) {
            this.nodeDpid2 = nodeDpid2;
            this.dpid2 = DeviceId.deviceId(nodeDpid2);
        }

        public DeviceId getDpid1() {
            return dpid1;
        }

        public void setDpid1(DeviceId dpid1) {
            this.dpid1 = dpid1;
            this.nodeDpid1 = dpid1.toString();
        }

        public DeviceId getDpid2() {
            return dpid2;
        }

        public void setDpid2(DeviceId dpid2) {
            this.dpid2 = dpid2;
            this.nodeDpid2 = dpid2.toString();
        }

        public Map<String, JsonNode> getParams() {
            return params;
        }

        public void setParams(Map<String, JsonNode> params) {
            this.params = params;
        }

        public Map<String, String> getPublishAttributes() {
            return publishAttributes;
        }

        public void setPublishAttributes(Map<String, String> publishAttributes) {
            this.publishAttributes = publishAttributes;
        }
    }
}

