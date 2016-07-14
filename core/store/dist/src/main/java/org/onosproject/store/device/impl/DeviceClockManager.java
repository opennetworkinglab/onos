/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.store.device.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceClockService;
import org.onosproject.store.Timestamp;
import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.slf4j.Logger;

/**
 * Clock service to issue Timestamp based on Device Mastership.
 */
@Component(immediate = true)
@Service
public class DeviceClockManager implements DeviceClockService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipTermService mastershipTermService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    protected NodeId localNodeId;

    private final AtomicLong ticker = new AtomicLong(0);

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Timestamp getTimestamp(DeviceId deviceId) {
        MastershipTerm term = mastershipTermService.getMastershipTerm(deviceId);
        if (term == null || !localNodeId.equals(term.master())) {
            throw new IllegalStateException("Requesting timestamp for " + deviceId + " without mastership");
        }
        return new MastershipBasedTimestamp(term.termNumber(), ticker.incrementAndGet());
    }

    @Override
    public boolean isTimestampAvailable(DeviceId deviceId) {
        MastershipTerm term = mastershipTermService.getMastershipTerm(deviceId);
        return term != null && localNodeId.equals(term.master());
    }
}
