/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtnrsc.portpairgroup.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.MultiValuedTimestamp;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupListener;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

/**
 * Provides implementation of the portPairGroupService.
 */
@Component(immediate = true)
@Service
public class PortPairGroupManager implements PortPairGroupService {

    private static final String PORT_PAIR_GROUP_ID_NULL = "PortPairGroup ID cannot be null";
    private static final String PORT_PAIR_GROUP_NULL = "PortPairGroup cannot be null";
    private static final String LISTENER_NOT_NULL = "Listener cannot be null";

    private final Logger log = getLogger(getClass());
    private final Set<PortPairGroupListener> listeners = Sets.newCopyOnWriteArraySet();
    private EventuallyConsistentMap<PortPairGroupId, PortPairGroup> portPairGroupStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Activate
    public void activate() {

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(MultiValuedTimestamp.class)
                .register(PortPairGroup.class);

        portPairGroupStore = storageService
                .<PortPairGroupId, PortPairGroup>eventuallyConsistentMapBuilder()
                .withName("portpairgroupstore").withSerializer(serializer)
                .withTimestampProvider((k, v) -> new WallClockTimestamp()).build();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        portPairGroupStore.destroy();
        listeners.clear();
        log.info("Stopped");
    }

    @Override
    public boolean exists(PortPairGroupId portPairGroupId) {
        checkNotNull(portPairGroupId, PORT_PAIR_GROUP_ID_NULL);
        return portPairGroupStore.containsKey(portPairGroupId);
    }

    @Override
    public int getPortPairGroupCount() {
        return portPairGroupStore.size();
    }

    @Override
    public Iterable<PortPairGroup> getPortPairGroups() {
        return Collections.unmodifiableCollection(portPairGroupStore.values());
    }

    @Override
    public PortPairGroup getPortPairGroup(PortPairGroupId portPairGroupId) {
        checkNotNull(portPairGroupId, PORT_PAIR_GROUP_ID_NULL);
        return portPairGroupStore.get(portPairGroupId);
    }

    @Override
    public boolean createPortPairGroup(PortPairGroup portPairGroup) {
        checkNotNull(portPairGroup, PORT_PAIR_GROUP_NULL);

        portPairGroupStore.put(portPairGroup.portPairGroupId(), portPairGroup);
        if (!portPairGroupStore.containsKey(portPairGroup.portPairGroupId())) {
            log.debug("The portPairGroup is created failed which identifier was {}", portPairGroup.portPairGroupId()
                      .toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean updatePortPairGroup(PortPairGroup portPairGroup) {
        checkNotNull(portPairGroup, PORT_PAIR_GROUP_NULL);

        if (!portPairGroupStore.containsKey(portPairGroup.portPairGroupId())) {
            log.debug("The portPairGroup is not exist whose identifier was {} ",
                      portPairGroup.portPairGroupId().toString());
            return false;
        }

        portPairGroupStore.put(portPairGroup.portPairGroupId(), portPairGroup);

        if (!portPairGroup.equals(portPairGroupStore.get(portPairGroup.portPairGroupId()))) {
            log.debug("The portPairGroup is updated failed whose identifier was {} ",
                      portPairGroup.portPairGroupId().toString());
            return false;
        }
        return true;
    }

    @Override
    public boolean removePortPairGroup(PortPairGroupId portPairGroupId) {
        checkNotNull(portPairGroupId, PORT_PAIR_GROUP_NULL);

        portPairGroupStore.remove(portPairGroupId);
        if (portPairGroupStore.containsKey(portPairGroupId)) {
            log.debug("The portPairGroup is removed failed whose identifier was {}",
                      portPairGroupId.toString());
            return false;
        }
        return true;
    }

    @Override
    public void addListener(PortPairGroupListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.add(listener);
    }

    @Override
    public void removeListener(PortPairGroupListener listener) {
        checkNotNull(listener, LISTENER_NOT_NULL);
        listeners.remove(listener);
    }
}
