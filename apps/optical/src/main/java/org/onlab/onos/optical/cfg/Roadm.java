package org.onlab.onos.optical.cfg;

/**
 * ROADM java data object converted from a JSON file.
 */
class Roadm {
    private String name;
    private String nodeID;
    private double longtitude;
    private double latitude;
    private int regenNum;

    //TODO use the following attributes when needed for configurations
    private int tPort10G;
    private int tPort40G;
    private int tPort100G;
    private int wPort;

    public Roadm() {
    }

    public Roadm(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setNodeId(String nameId) {
        this.nodeID = nameId;
    }

    public String getNodeId() {
        return this.nodeID;
    }

    public void setLongtitude(double x) {
        this.longtitude = x;
    }

    public double getLongtitude() {
        return this.longtitude;
    }

    public void setLatitude(double y) {
        this.latitude = y;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public void setRegenNum(int num) {
        this.regenNum = num;
    }
    public int getRegenNum() {
        return this.regenNum;
    }

    public void setTport10GNum(int num) {
        this.tPort10G = num;
    }
    public int getTport10GNum() {
        return this.tPort10G;
    }

    public void setTport40GNum(int num) {
        this.tPort40G = num;
    }
    public int getTport40GNum() {
        return this.tPort40G;
    }

    public void setTport100GNum(int num) {
        this.tPort100G = num;
    }
    public int getTport100GNum() {
        return this.tPort100G;
    }

    public void setWportNum(int num) {
        this.wPort = num;
    }
    public int getWportNum() {
        return this.wPort;
    }

    @Override
    public String toString() {
        return new StringBuilder(" ROADM Name: ").append(this.name)
                .append(" nodeID: ").append(this.nodeID)
                .append(" longtitude: ").append(this.longtitude)
                .append(" latitude: ").append(this.latitude)
                .append(" regenNum: ").append(this.regenNum)
                .append(" 10GTportNum: ").append(this.tPort10G)
                .append(" 40GTportNum: ").append(this.tPort40G)
                .append(" 100GTportNum: ").append(this.tPort100G)
                .append(" WportNum: ").append(this.wPort).toString();
    }
}

