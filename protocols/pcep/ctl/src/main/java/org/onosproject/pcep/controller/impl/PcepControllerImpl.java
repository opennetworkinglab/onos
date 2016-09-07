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
package org.onosproject.pcep.controller.impl;

import com.google.common.collect.Sets;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.DeviceId;
import org.onosproject.pcep.api.PcepController;
import org.onosproject.pcep.api.PcepDpid;
import org.onosproject.pcep.api.PcepLinkListener;
import org.onosproject.pcep.api.PcepSwitch;
import org.onosproject.pcep.api.PcepSwitchListener;
import org.onosproject.pcep.api.PcepTunnel;
import org.onosproject.pcep.api.PcepTunnelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Implementation of PCEP controller [protocol].
 */
@Component(immediate = true)
@Service
public class PcepControllerImpl implements PcepController {

    private static final Logger log = LoggerFactory.getLogger(PcepControllerImpl.class);

    protected Set<PcepTunnelListener> pcepTunnelListener = Sets.newHashSet();
    protected Set<PcepLinkListener> pcepLinkListener = Sets.newHashSet();
    protected Set<PcepSwitchListener> pcepSwitchListener = Sets.newHashSet();

    private final Controller ctrl = new Controller();

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Iterable<PcepSwitch> getSwitches() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PcepSwitch getSwitch(PcepDpid did) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addListener(PcepSwitchListener listener) {
        this.pcepSwitchListener.add(listener);
    }

    @Override
    public void removeListener(PcepSwitchListener listener) {
        this.pcepSwitchListener.remove(listener);
    }

    @Override
    public void addLinkListener(PcepLinkListener listener) {
        this.pcepLinkListener.add(listener);
    }

    @Override
    public void removeLinkListener(PcepLinkListener listener) {
        this.pcepLinkListener.remove(listener);
    }

    @Override
    public void addTunnelListener(PcepTunnelListener listener) {
        this.pcepTunnelListener.add(listener);
    }

    @Override
    public void removeTunnelListener(PcepTunnelListener listener) {
        this.pcepTunnelListener.remove(listener);
    }

    @Override
    public PcepTunnel applyTunnel(DeviceId srcDid, DeviceId dstDid, long srcPort, long dstPort, long bandwidth,
            String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean deleteTunnel(String id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Boolean updateTunnelBandwidth(String id, long bandwidth) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void getTunnelStatistics(String pcepTunnelId) {
        // TODO Auto-generated method stub
    }
}
