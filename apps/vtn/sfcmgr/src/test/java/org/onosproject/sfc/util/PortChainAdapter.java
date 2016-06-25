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

import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnrsc.portchain.PortChainEvent;
import org.onosproject.vtnrsc.portchain.PortChainListener;
import org.onosproject.event.AbstractListenerManager;

/**
 * Provides implementation of the portChainService.
 */
public class PortChainAdapter
            extends AbstractListenerManager<PortChainEvent, PortChainListener>
            implements PortChainService {

    private ConcurrentMap<PortChainId, PortChain> portChainStore = new ConcurrentHashMap<>();

    @Override
    public boolean exists(PortChainId portChainId) {
        return portChainStore.containsKey(portChainId);
    }

    @Override
    public int getPortChainCount() {
        return portChainStore.size();
    }

    @Override
    public Iterable<PortChain> getPortChains() {
        return Collections.unmodifiableCollection(portChainStore.values());
    }

    @Override
    public PortChain getPortChain(PortChainId portChainId) {
        return portChainStore.get(portChainId);
    }

    @Override
    public boolean createPortChain(PortChain portChain) {
        portChainStore.put(portChain.portChainId(), portChain);
        if (!portChainStore.containsKey(portChain.portChainId())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean updatePortChain(PortChain portChain) {
        if (!portChainStore.containsKey(portChain.portChainId())) {
            return false;
        }

        portChainStore.put(portChain.portChainId(), portChain);

        if (!portChain.equals(portChainStore.get(portChain.portChainId()))) {
            return false;
        }
        return true;
    }

    @Override
    public boolean removePortChain(PortChainId portChainId) {
        return true;
    }
}
