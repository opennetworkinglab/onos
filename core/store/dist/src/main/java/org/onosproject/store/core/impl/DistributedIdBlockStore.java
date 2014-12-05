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
package org.onosproject.store.core.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.IdBlock;
import org.onosproject.core.IdBlockStore;
import org.onosproject.store.hz.StoreService;

import java.util.Map;

/**
 * Distributed implementation of id block store using Hazelcast.
 */
@Component(immediate = true)
@Service
public class DistributedIdBlockStore implements IdBlockStore {

    private static final long DEFAULT_BLOCK_SIZE = 0x1000L;

    protected Map<String, IAtomicLong> topicBlocks;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StoreService storeService;

    protected HazelcastInstance theInstance;

    @Activate
    public void activate() {
        theInstance = storeService.getHazelcastInstance();
    }

    @Override
    public IdBlock getIdBlock(String topic) {
        Long blockBase = theInstance.getAtomicLong(topic).getAndAdd(DEFAULT_BLOCK_SIZE);
        return new IdBlock(blockBase, DEFAULT_BLOCK_SIZE);
    }
}
