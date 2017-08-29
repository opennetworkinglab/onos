/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.l3vpn.netl3vpn;

/**
 * Represents the tunnel information.
 */
public class TunnelInfo {

    /**
     * Destination ip address.
     */
    private final String desIp;

    /**
     * Tunnel name.
     */
    private final String tnlName;

    /**
     * Tunnel policy name.
     */
    private final String polName;

    /**
     * Device id.
     */
    private final String devId;

    /**
     * Level of the model.
     */
    private ModelIdLevel level;

    /**
     * Creates tunnel info with destination ip address, tunnel name, tunnel
     * policy name and device id.
     *
     * @param dIp   destination ip
     * @param tName tunnel name
     * @param pName tunnel policy name
     * @param dId   device id
     */
    public TunnelInfo(String dIp, String tName, String pName, String dId) {
        this.desIp = dIp;
        this.tnlName = tName;
        this.polName = pName;
        this.devId = dId;
    }

    /**
     * Returns the destination ip-address.
     *
     * @return destination ip-address
     */
    public String desIp() {
        return desIp;
    }

    /**
     * Returns the tunnel name.
     *
     * @return tunnel name
     */
    public String tnlName() {
        return tnlName;
    }

    /**
     * Returns the tunnel policy name.
     *
     * @return tunnel policy name
     */
    public String polName() {
        return polName;
    }

    /**
     * Returns the device id.
     *
     * @return device id
     */
    public String devId() {
        return devId;
    }

    /**
     * Returns the model id level.
     *
     * @return model id level
     */
    public ModelIdLevel level() {
        return level;
    }

    /**
     * Sets the model id level.
     *
     * @param level model id level
     */
    public void level(ModelIdLevel level) {
        this.level = level;
    }
}
