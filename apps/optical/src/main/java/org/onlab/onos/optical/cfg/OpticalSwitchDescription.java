package org.onlab.onos.optical.cfg;

import java.util.Map;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonProperty;
import org.onlab.util.HexString;

/**
 * Public class corresponding to JSON described data model.
 */
public class OpticalSwitchDescription {
    protected String name;
    protected long dpid;
    protected String nodeDpid;
    protected String type;
    protected double latitude;
    protected double longitude;
    protected boolean allowed;
    protected Map<String, JsonNode> params;
    protected Map<String, String> publishAttributes;

    public String getName() {
        return name;
    }
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    public long getDpid() {
        return dpid;
    }
    @JsonProperty("dpid")
    public void setDpid(long dpid) {
        this.dpid = dpid;
        this.nodeDpid = HexString.toHexString(dpid);
    }

    public String getNodeDpid() {
        return nodeDpid;
    }

    public String getHexDpid() {
        return nodeDpid;
    }

    public void setNodeDpid(String nodeDpid) {
        this.nodeDpid = nodeDpid;
        this.dpid = HexString.toLong(nodeDpid);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
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
