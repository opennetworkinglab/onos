package org.onlab.onos.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a set of addresses bound to a port.
 */
public class AddressEntry {
    private String dpid;
    private short portNumber;
    private List<String> ipAddresses;
    private String macAddress;

    public String getDpid() {
        return dpid;
    }

    @JsonProperty("dpid")
    public void setDpid(String strDpid) {
        this.dpid = strDpid;
    }

    public short getPortNumber() {
        return portNumber;
    }

    @JsonProperty("port")
    public void setPortNumber(short portNumber) {
        this.portNumber = portNumber;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    @JsonProperty("ips")
    public void setIpAddresses(List<String> strIps) {
        this.ipAddresses = strIps;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @JsonProperty("mac")
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
