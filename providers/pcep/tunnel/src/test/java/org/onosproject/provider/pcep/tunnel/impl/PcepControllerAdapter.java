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

package org.onosproject.provider.pcep.tunnel.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.pcep.api.PcepController;
import org.onosproject.pcep.api.PcepDpid;
import org.onosproject.pcep.api.PcepLinkListener;
import org.onosproject.pcep.api.PcepSwitch;
import org.onosproject.pcep.api.PcepSwitchListener;
import org.onosproject.pcep.api.PcepTunnel;
import org.onosproject.pcep.api.PcepTunnelListener;

public class PcepControllerAdapter implements PcepController {

    @Override
    public Iterable<PcepSwitch> getSwitches() {
        return null;
    }

    @Override
    public PcepSwitch getSwitch(PcepDpid did) {
        return null;
    }

    @Override
    public void addListener(PcepSwitchListener listener) {

    }

    @Override
    public void removeListener(PcepSwitchListener listener) {
    }

    @Override
    public void addLinkListener(PcepLinkListener listener) {
    }

    @Override
    public void removeLinkListener(PcepLinkListener listener) {
    }

    @Override
    public void addTunnelListener(PcepTunnelListener listener) {
    }

    @Override
    public void removeTunnelListener(PcepTunnelListener listener) {
    }

    @Override
    public PcepTunnel applyTunnel(DeviceId srcDid, DeviceId dstDid, long srcPort, long dstPort, long bandwidth,
                                  String name) {
        return null;
    }

    @Override
    public Boolean deleteTunnel(String id) {
        return null;
    }

    @Override
    public Boolean updateTunnelBandwidth(String id, long bandwidth) {
        return null;
    }

    @Override
    public void getTunnelStatistics(String pcepTunnelId) {

    }
}
