/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.pcep.api;

/**
 * Description of a pcep tunnel hop node.a hop list consists of a number of hop
 * node.
 */
public class PcepHopNodeDescription {
    private PcepDpid deviceId;
    private long portNum;

    /**
     * Get the pcepdpid of a node.
     *
     * @return device pcepdpid.
     */
    public PcepDpid getDeviceId() {
        return deviceId;
    }

    /**
     * Set the pcepdpid of a node.
     *
     * @param deviceId pcep dpid of a node.
     */
    public void setDeviceId(PcepDpid deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * Get the port number of a node.
     *
     * @return port number.
     */
    public long getPortNum() {
        return portNum;
    }

    /**
     * Set the port number of a node.
     *
     * @param portNum port number of a node.
     */
    public void setPortNum(long portNum) {
        this.portNum = portNum;
    }

}
