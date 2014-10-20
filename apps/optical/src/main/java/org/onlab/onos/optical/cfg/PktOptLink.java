package org.onlab.onos.optical.cfg;

/**
 * Packet-optical link Java data object.
 */
class PktOptLink {
    private String srcNodeName;
    private String snkNodeName;
    private String srcNodeId;
    private String snkNodeId;
    private int srcPort;
    private int snkPort;
    private double bandwidth;
    private double cost;
    private long adminWeight;

    public PktOptLink(String srcName, String snkName) {
        this.srcNodeName = srcName;
        this.snkNodeName = snkName;
    }

    public PktOptLink() {
        // TODO Auto-generated constructor stub
    }

    public void setSrcNodeName(String name) {
        this.srcNodeName = name;
    }

    public String getSrcNodeName() {
        return this.srcNodeName;
    }

    public void setSnkNodeName(String name) {
        this.snkNodeName = name;
    }

    public String getSnkNodeName() {
        return this.snkNodeName;
    }

    public void setSrcNodeId(String nodeId) {
        this.srcNodeId = nodeId;
    }

    public String getSrcNodeId() {
        return this.srcNodeId;
    }

    public void setSnkNodeId(String nodeId) {
        this.snkNodeId = nodeId;
    }

    public String getSnkNodeId() {
        return this.snkNodeId;
    }

    public void setSrcPort(int port) {
        this.srcPort = port;
    }

    public int getSrcPort() {
        return this.srcPort;
    }

    public void setSnkPort(int port) {
        this.snkPort = port;
    }

    public int getSnkPort() {
        return this.snkPort;
    }

    public void setBandwdith(double x) {
        this.bandwidth = x;
    }

    public double getBandwidth() {
        return this.bandwidth;
    }

    public void setCost(double x) {
        this.cost = x;
    }

    public double getCost() {
        return this.cost;
    }

    public void setAdminWeight(long x) {
        this.adminWeight = x;
    }

    public long getAdminWeight() {
        return this.adminWeight;
    }

    @Override
    public String toString() {
        return new StringBuilder(" srcNodeName: ").append(this.srcNodeName)
                .append(" snkNodeName: ").append(this.snkNodeName)
                .append(" srcNodeId: ").append(this.srcNodeId)
                .append(" snkNodeId: ").append(this.snkNodeId)
                .append(" srcPort: ").append(this.srcPort)
                .append(" snkPort: ").append(this.snkPort)
                .append(" bandwidth: ").append(this.bandwidth)
                .append(" cost: ").append(this.cost)
                .append(" adminWeight: ").append(this.adminWeight).toString();
    }
}
