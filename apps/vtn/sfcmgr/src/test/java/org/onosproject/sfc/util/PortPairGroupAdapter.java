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
package org.onosproject.sfc.util;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;

import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupListener;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;

/**
 * Provides implementation of the portPairGroupService.
 */
public class PortPairGroupAdapter implements PortPairGroupService {

    private ConcurrentMap<PortPairGroupId, PortPairGroup> portPairGroupStore = new ConcurrentHashMap<>();

    @Override
    public boolean exists(PortPairGroupId portPairGroupId) {
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
        return portPairGroupStore.get(portPairGroupId);
    }

    @Override
    public boolean createPortPairGroup(PortPairGroup portPairGroup) {
        portPairGroupStore.put(portPairGroup.portPairGroupId(), portPairGroup);
        if (!portPairGroupStore.containsKey(portPairGroup.portPairGroupId())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean updatePortPairGroup(PortPairGroup portPairGroup) {
        if (!portPairGroupStore.containsKey(portPairGroup.portPairGroupId())) {
            return false;
        }

        portPairGroupStore.put(portPairGroup.portPairGroupId(), portPairGroup);

        if (!portPairGroup.equals(portPairGroupStore.get(portPairGroup.portPairGroupId()))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean removePortPairGroup(PortPairGroupId portPairGroupId) {
        return true;
    }

    @Override
    public void addListener(PortPairGroupListener listener) {
    }

    @Override
    public void removeListener(PortPairGroupListener listener) {
    }
}
