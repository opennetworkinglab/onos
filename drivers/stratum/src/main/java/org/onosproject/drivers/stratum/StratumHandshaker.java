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
import org.onosproject.drivers.gnoi.GnoiHandshaker;
import org.onosproject.drivers.p4runtime.P4RuntimeHandshaker;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.device.DeviceAgentListener;
import org.onosproject.net.device.DeviceHandshaker;
import org.onosproject.net.provider.ProviderId;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of DeviceHandshaker for Stratum device.
 */
public class StratumHandshaker
        extends AbstractStratumBehaviour<DeviceHandshaker>
        implements DeviceHandshaker {

    public StratumHandshaker() {
        super(new P4RuntimeHandshaker(), new GnmiHandshaker(), new GnoiHandshaker());
    }

    @Override
    public boolean connect() {
        return p4runtime.connect() && gnmi.connect() && gnoi.connect();
    }

    @Override
    public boolean hasConnection() {
        return p4runtime.hasConnection() && gnmi.hasConnection() && gnoi.hasConnection();
    }

    @Override
    public void disconnect() {
        p4runtime.disconnect();
        gnmi.disconnect();
        gnoi.disconnect();
    }

    @Override
    public boolean isReachable() {
        // Reachability is mainly used for mastership contests and it's a
        // prerequisite for availability. We can probably live without gNMI and
        // gNOI, but we will always need P4Runtime.
        return p4runtime.isReachable();
    }

    @Override
    public CompletableFuture<Boolean> probeReachability() {
        // p4runtime probe reachability is based on GetPipelineConfig gRPC that
        // can timeout if we are setting in parallel the pipeline: the two requests
        // can concur for the same lock. For our purposes it is enough to check if
        // the device is still there; for this reason stratum handshaker now relies
        // on gNOI reachability which is based on getTime RPC.
        return gnoi.probeReachability();
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
}
