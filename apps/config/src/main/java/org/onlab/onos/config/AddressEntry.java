package org.onlab.onos.config;

import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

/**
 * Represents a set of addresses bound to a port.
 */
public class AddressEntry {
    private String dpid;
    private short portNumber;
    private List<IpPrefix> ipAddresses;
    private MacAddress macAddress;

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

    public List<IpPrefix> getIpAddresses() {
        return ipAddresses;
    }

    @JsonProperty("ips")
    public void setIpAddresses(List<IpPrefix> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public MacAddress getMacAddress() {
        return macAddress;
    }

    @JsonProperty("mac")
    public void setMacAddress(MacAddress macAddress) {
        this.macAddress = macAddress;
    }
}
