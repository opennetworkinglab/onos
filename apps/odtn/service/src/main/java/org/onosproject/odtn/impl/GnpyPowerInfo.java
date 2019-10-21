/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.odtn.impl;

import com.google.common.annotations.Beta;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.OchSignal;

import java.util.List;
import java.util.Map;

/**
 * This class contains information given by gNPY for a given path.
 */
@Beta
public final class GnpyPowerInfo {

    private Map<DeviceId, Double> deviceAtoBPowerMap;
    private Map<DeviceId, Double> deviceBtoAPowerMap;
    private double launchPower;
    private List<Link> path;
    private ConnectPoint ingress;
    private ConnectPoint egress;
    private OchSignal ochSignal;

    /**
     * Creates the class with information.
     *
     * @param deviceAtoBPowerMap the power in a to b direction
     * @param deviceBtoAPowerMap the power in b to a direction
     * @param launchPower        the power at the TXs
     * @param path               the pat
     * @param ingress            the ingress device (A)
     * @param egress             the egress device (B)
     * @param ochSignal          the signal
     */
    public GnpyPowerInfo(Map<DeviceId, Double> deviceAtoBPowerMap,
                         Map<DeviceId, Double> deviceBtoAPowerMap, double launchPower,
                         List<Link> path, ConnectPoint ingress, ConnectPoint egress,
                         OchSignal ochSignal) {
        this.deviceAtoBPowerMap = deviceAtoBPowerMap;
        this.deviceBtoAPowerMap = deviceBtoAPowerMap;
        this.launchPower = launchPower;
        this.path = path;
        this.ingress = ingress;
        this.egress = egress;
        this.ochSignal = ochSignal;
    }

    /**
     * Retrieve the ingress connect point of the path (A).
     *
     * @return ingress connect point
     */
    public ConnectPoint ingress() {
        return ingress;
    }

    /**
     * Retrieve the egress connect point of the path (B).
     *
     * @return egress connect point
     */
    public ConnectPoint egress() {
        return egress;
    }

    /**
     * Retrieve the ochSignal.
     *
     * @return signal
     */
    public OchSignal ochSignal() {
        return ochSignal;
    }

    /**
     * Retrieve the the power in a to b direction.
     *
     * @return power map a to b
     */
    public Map<DeviceId, Double> deviceAtoBPowerMap() {
        return deviceAtoBPowerMap;
    }

    /**
     * Retrieve the power in b to a direction.
     *
     * @return power map b to a
     */
    public Map<DeviceId, Double> deviceBtoAPowerMap() {
        return deviceBtoAPowerMap;
    }

    /**
     * Retrieve the launch power at the TX, both A and B.
     *
     * @return ingress connect point
     */
    public double launchPower() {
        return launchPower;
    }

    /**
     * Retrieve the set of links for the path in the network.
     *
     * @return links fo the path
     */
    public List<Link> path() {
        return path;
    }
}
