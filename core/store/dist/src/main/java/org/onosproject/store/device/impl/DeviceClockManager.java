/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipEvent.Type;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceClockService;
import org.onosproject.store.Timestamp;
import org.onosproject.store.impl.MastershipBasedTimestamp;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Clock service to issue Timestamp based on Device Mastership.
 */
@Component(immediate = true, service = DeviceClockService.class)
public class DeviceClockManager implements DeviceClockService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipTermService mastershipTermService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    protected NodeId localNodeId;

    private final AtomicLong ticker = new AtomicLong(0);

    // Map from DeviceId -> last known term number for this node.
    // using Cache class but using it as Map which will age out old entries
    private final Cache<DeviceId, Long> myLastKnownTerm =
            CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();

    private MastershipListener listener;

    @Activate
    public void activate() {
        localNodeId = clusterService.getLocalNode().id();

        listener = new InnerMastershipListener();
        mastershipService.addListener(listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        mastershipService.removeListener(listener);
        log.info("Stopped");
    }

    @Override
    public Timestamp getTimestamp(DeviceId deviceId) {
        Long termNumber = refreshLastKnownTerm(deviceId);
        if (termNumber == null) {
            log.warn("Requested timestamp for {} which {}"
                    + "doesn't have known recent mastership term",
                    deviceId, localNodeId);
            throw new IllegalStateException("Requesting timestamp for " + deviceId + " without mastership");
        }
        return new MastershipBasedTimestamp(termNumber, ticker.incrementAndGet());
    }

    @Override
    public boolean isTimestampAvailable(DeviceId deviceId) {
        return myLastKnownTerm.getIfPresent(deviceId) != null ||
               refreshLastKnownTerm(deviceId) != null;
    }

    /**
     * Refreshes this node's last known term number to the latest state.
     *
     * @param deviceId of the Device to refresh mastership term
     * @return latest mastership term number or null if this node
     *         did not have a term number recently
     */
    private Long refreshLastKnownTerm(DeviceId deviceId) {
        MastershipTerm term = mastershipTermService.getMastershipTerm(deviceId);
        return myLastKnownTerm.asMap().compute(deviceId, (key, old) -> {
            if (old == null) {
                return Optional.ofNullable(term)
                            .filter(t -> localNodeId.equals(t.master()))
                            .map(MastershipTerm::termNumber)
                            .orElse(null);
            }
            return Optional.ofNullable(term)
                    .filter(t -> localNodeId.equals(t.master()))
                    .map(MastershipTerm::termNumber)
                    // TODO make following integer wrap-safe
                    .map(tn -> Math.max(old, tn))
                    .orElse(old);
        });
    }

    /**
     * Refreshes {@link DeviceClockManager#myLastKnownTerm} on Master update event.
     */
    private final class InnerMastershipListener implements MastershipListener {

        @Override
        public boolean isRelevant(MastershipEvent event) {
            return event.type() == Type.MASTER_CHANGED;
        }

        @Override
        public void event(MastershipEvent event) {
            if (localNodeId.equals(event.roleInfo().master())) {
                refreshLastKnownTerm(event.subject());
            }
        }
    }
}
