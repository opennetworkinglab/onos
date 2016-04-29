/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.castor;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * POJO class for the Peer and the Route Servers.
 */

@XmlRootElement
public class Peer {

    private String name;
    private String dpid;
    private String ipAddress;
    private String port;
    private boolean l2;

    public Peer() {}

    public Peer(String name, String dpid, String ipAddress, String port) {
        this.name = name;
        this.dpid = dpid;
        this.ipAddress = ipAddress;
        this.port = port;
        this.l2 = false;
    }

    public void setDpid(String dpid) {
        this.dpid = dpid;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setPort(String port) {
        this.port = port;
    }

    /**
     * The name of the Peer or Customer to be added.
     *
     * @param name A String name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Specifies if the layer two flows for this peer are configured or not.
     *
     * @param value True if layer two configured.
     */
    public void setL2(boolean value) {
        this.l2 = value;
    }

    /**
     * Returns the name of the Peer or the Customer.
     *
     * @return The String name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the IP Address of the Peer.
     *
     * @return IP Address.
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * Returns the port number where the Peer is attached.
     *
     * @return String Connect Point
     */
    public String getPort() {
        return port;
    }

    /**
     * Returns the layer two status of the Peer.
     *
     * @return True if layer two set.
     */
    public boolean getl2Status() {
        return l2;
    }

    public String getDpid() {
        return dpid;
    }

    @Override
    public boolean equals(Object ob) {
        if (ob == null) {
            return false;
        }
        Peer other = (Peer) ob;
        if (this.ipAddress.equals(other.ipAddress)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.ipAddress != null ? this.ipAddress.hashCode() : 0);
        return hash;
    }
}
