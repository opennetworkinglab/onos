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

import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.portpair.PortPairListener;
import org.onosproject.vtnrsc.portpair.PortPairService;

/**
 * Provides implementation of the portPairService.
 */
public class PortPairAdapter implements PortPairService {

    private ConcurrentMap<PortPairId, PortPair> portPairStore = new ConcurrentHashMap<>();

    @Override
    public boolean exists(PortPairId portPairId) {
        return portPairStore.containsKey(portPairId);
    }

    @Override
    public int getPortPairCount() {
        return portPairStore.size();
    }

    @Override
    public Iterable<PortPair> getPortPairs() {
        return Collections.unmodifiableCollection(portPairStore.values());
    }

    @Override
    public PortPair getPortPair(PortPairId portPairId) {
        return portPairStore.get(portPairId);
    }

    @Override
    public boolean createPortPair(PortPair portPair) {
        portPairStore.put(portPair.portPairId(), portPair);
        if (!portPairStore.containsKey(portPair.portPairId())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean updatePortPair(PortPair portPair) {
        if (!portPairStore.containsKey(portPair.portPairId())) {
            return false;
        }

        portPairStore.put(portPair.portPairId(), portPair);

        if (!portPair.equals(portPairStore.get(portPair.portPairId()))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean removePortPair(PortPairId portPairId) {
        return true;
    }

    @Override
    public void addListener(PortPairListener listener) {
    }

    @Override
    public void removeListener(PortPairListener listener) {
    }
}
