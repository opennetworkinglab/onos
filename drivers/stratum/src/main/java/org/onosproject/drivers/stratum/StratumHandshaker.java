/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.drivers.stratum;

import org.onosproject.drivers.gnmi.GnmiHandshaker;
import org.onosproject.drivers.p4runtime.P4RuntimeHandshaker;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverData;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.provider.ProviderId;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DeviceHandshaker for Stratum device.
 */
public class StratumHandshaker extends AbstractHandlerBehaviour implements DeviceHandshaker {

    private P4RuntimeHandshaker p4runtime;
    private GnmiHandshaker gnmi;

    public StratumHandshaker() {
        p4runtime = new P4RuntimeHandshaker();
        gnmi = new GnmiHandshaker();
    }

    @Override
    public void setHandler(DriverHandler handler) {
        super.setHandler(handler);
        p4runtime.setHandler(handler);
        gnmi.setHandler(handler);
    }

    @Override
    public void setData(DriverData data) {
        super.setData(data);
        p4runtime.setData(data);
        gnmi.setData(data);
    }

    @Override
    public boolean isReachable() {
        return p4runtime.isReachable() && gnmi.isReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeReachability() {
        return p4runtime.probeReachability();
    }

    @Override
    public boolean isAvailable() {
        // Availability concerns packet forwarding, hence we consider only
        // P4Runtime.
        return p4runtime.isAvailable();
    }

    @Override
    public CompletableFuture<Boolean> probeAvailability() {
        return p4runtime.probeAvailability();
    }

    @Override
    public void roleChanged(MastershipRole newRole) {
        p4runtime.roleChanged(newRole);
    }

    @Override
    public void roleChanged(int preference, long term) {
        p4runtime.roleChanged(preference, term);
    }

    @Override
    public MastershipRole getRole() {
        return p4runtime.getRole();
    }

    @Override
    public void addDeviceAgentListener(ProviderId providerId, DeviceAgentListener listener) {
        p4runtime.addDeviceAgentListener(providerId, listener);
    }

    @Override
    public void removeDeviceAgentListener(ProviderId providerId) {
        p4runtime.removeDeviceAgentListener(providerId);
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        // We should execute connections in parallel.
        return p4runtime.connect().thenCombine(gnmi.connect(), Boolean::logicalAnd);
    }

    @Override
    public boolean isConnected() {
        return p4runtime.isConnected() && gnmi.isConnected();
    }

    @Override
    public void disconnect() {
        p4runtime.disconnect();
        gnmi.disconnect();
    }
}
