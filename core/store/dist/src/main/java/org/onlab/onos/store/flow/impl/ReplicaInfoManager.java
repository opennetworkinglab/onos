/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.store.flow.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onlab.onos.store.flow.ReplicaInfoEvent.Type.MASTER_CHANGED;

import java.util.Collections;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.event.AbstractListenerRegistry;
import org.onlab.onos.event.EventDeliveryService;
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipListener;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.flow.ReplicaInfo;
import org.onlab.onos.store.flow.ReplicaInfoEvent;
import org.onlab.onos.store.flow.ReplicaInfoEventListener;
import org.onlab.onos.store.flow.ReplicaInfoService;
import org.slf4j.Logger;

/**
 * Manages replica placement information.
 */
@Component(immediate = true)
@Service
public class ReplicaInfoManager implements ReplicaInfoService {

    private final Logger log = getLogger(getClass());

    private final MastershipListener mastershipListener = new InternalMastershipListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventDeliveryService eventDispatcher;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    protected final AbstractListenerRegistry<ReplicaInfoEvent, ReplicaInfoEventListener>
        listenerRegistry = new AbstractListenerRegistry<>();

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
        // TODO: populate backup List when we reach the point we need them.
        return new ReplicaInfo(mastershipService.getMasterFor(deviceId),
                               Collections.<NodeId>emptyList());
    }

    @Override
    public void addListener(ReplicaInfoEventListener listener) {
        listenerRegistry.addListener(checkNotNull(listener));
    }

    @Override
    public void removeListener(ReplicaInfoEventListener listener) {
        listenerRegistry.removeListener(checkNotNull(listener));
    }

    final class InternalMastershipListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            // TODO: distinguish stby list update, when MastershipService,
            //       start publishing them
            final List<NodeId> standbyList = Collections.<NodeId>emptyList();
            eventDispatcher.post(new ReplicaInfoEvent(MASTER_CHANGED,
                                event.subject(),
                                new ReplicaInfo(event.roleInfo().master(), standbyList)));
        }
    }

}
