/*
 * Copyright 2014-2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents a set of addresses bound to a port.
 */
public class AddressEntry {
    private String dpid;
    private long portNumber;
    private List<String> ipAddresses;
    private String macAddress;
    private Short vlan;

    public String getDpid() {
        return dpid;
    }

    @JsonProperty("dpid")
    public void setDpid(String strDpid) {
        this.dpid = strDpid;
    }

    public long getPortNumber() {
        return portNumber;
    }

    @JsonProperty("port")
    public void setPortNumber(long portNumber) {
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

    public Short getVlan() {
        return vlan;
    }

    @JsonProperty("vlan")
    public void setVlan(short vlan) {
        this.vlan = vlan;
    }
}
