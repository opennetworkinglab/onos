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

package org.onosproject.store.host.impl;

import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.Host;
import org.onosproject.net.host.ProbeMode;
import org.onosproject.net.host.HostProbe;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Internal data structure to record the info of a host with location that is under verification.
 */
class DefaultHostProbe extends DefaultHost implements HostProbe {
    private ConnectPoint connectPoint;
    private int retry;
    private ProbeMode mode;
    private MacAddress probeMac;

    /**
     * Constructs DefaultHostProbe with given retry.
     *
     * @param host host to be probed
     * @param connectPoint location to be verified
     * @param probeMac source MAC address of the probe
     * @param mode probe mode
     * @param retry number of retry
     */
    DefaultHostProbe(Host host, ConnectPoint connectPoint, ProbeMode mode, MacAddress probeMac, int retry) {
        super(host.providerId(), host.id(), host.mac(), host.vlan(), host.locations(), host.ipAddresses(),
                host.configured());

        this.connectPoint = connectPoint;
        this.mode = mode;
        this.probeMac = probeMac;
        this.retry = retry;
    }

    @Override
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    @Override
    public int retry() {
        return retry;
    }

    @Override
    public void decreaseRetry() {
        this.retry -= 1;
    }

    @Override
    public ProbeMode mode() {
        return mode;
    }

    @Override
    public MacAddress probeMac() {
        return probeMac;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefaultHostProbe)) {
            return false;
        }
        DefaultHostProbe that = (DefaultHostProbe) o;
        return (super.equals(o) &&
                Objects.equals(this.connectPoint, that.connectPoint) &&
                Objects.equals(this.retry, that.retry) &&
                Objects.equals(this.mode, that.mode)) &&
                Objects.equals(this.probeMac, that.probeMac);
    }
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), connectPoint, retry, mode, probeMac);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("host", super.toString())
                .add("location", connectPoint)
                .add("retry", retry)
                .add("mode", mode)
                .add("probeMac", probeMac)
                .toString();
    }
}
