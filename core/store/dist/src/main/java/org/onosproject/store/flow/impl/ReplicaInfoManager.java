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
package org.onosproject.store.flow.impl;

import org.onosproject.event.EventDeliveryService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipInfo;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.store.flow.ReplicaInfo;
import org.onosproject.store.flow.ReplicaInfoEvent;
import org.onosproject.store.flow.ReplicaInfoEventListener;
import org.onosproject.store.flow.ReplicaInfoService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.store.flow.ReplicaInfoEvent.Type.BACKUPS_CHANGED;
import static org.onosproject.store.flow.ReplicaInfoEvent.Type.MASTER_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Manages replica placement information.
 */
@Component(immediate = true, service = ReplicaInfoService.class)
public class ReplicaInfoManager implements ReplicaInfoService {

    private final Logger log = getLogger(getClass());

    private final MastershipListener mastershipListener = new InternalMastershipListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    protected final ListenerRegistry<ReplicaInfoEvent, ReplicaInfoEventListener>
        listenerRegistry = new ListenerRegistry<>();

    @Activate
    public void activate() {
        eventDispatcher.addSink(ReplicaInfoEvent.class, listenerRegistry);
        mastershipService.addListener(mastershipListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(ReplicaInfoEvent.class);
        mastershipService.removeListener(mastershipListener);
        log.info("Stopped");
    }

    @Override
    public ReplicaInfo getReplicaInfoFor(DeviceId deviceId) {
        return buildFromRoleInfo(mastershipService.getMastershipFor(deviceId));
    }

    @Override
    public void addListener(ReplicaInfoEventListener listener) {
        listenerRegistry.addListener(checkNotNull(listener));
    }

    @Override
    public void removeListener(ReplicaInfoEventListener listener) {
        listenerRegistry.removeListener(checkNotNull(listener));
    }

    private static ReplicaInfo buildFromRoleInfo(MastershipInfo mastership) {
        return new ReplicaInfo(mastership.term(), mastership.master().orElse(null), mastership.backups());
    }

    final class InternalMastershipListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            final ReplicaInfo replicaInfo = buildFromRoleInfo(event.mastershipInfo());
            switch (event.type()) {
                case MASTER_CHANGED:
                    eventDispatcher.post(new ReplicaInfoEvent(MASTER_CHANGED,
                        event.subject(),
                        replicaInfo));
                    break;
                case BACKUPS_CHANGED:
                    eventDispatcher.post(new ReplicaInfoEvent(BACKUPS_CHANGED,
                        event.subject(),
                        replicaInfo));
                    break;
                default:
                    break;
            }
        }
    }

}
