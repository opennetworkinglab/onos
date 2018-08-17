/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.store.intent.impl;

import com.google.common.annotations.Beta;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.IntentSetMultimap;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A collection that maps Intent IDs as keys to values as Intent IDs,
 * where each key may associated with multiple values without duplication.
 */
@Component(immediate = true, service = IntentSetMultimap.class)
@Beta
public class ConsistentIntentSetMultimap implements IntentSetMultimap {
    private final Logger log = getLogger(getClass());

    private static final String INTENT_MAPPING = "onos-intent-mapping";

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

    private ConsistentMap<IntentId, Set<IntentId>> intentMapping;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        intentMapping = storageService.<IntentId, Set<IntentId>>consistentMapBuilder()
                .withName(INTENT_MAPPING)
                .withSerializer(SERIALIZER)
                .build();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Set<IntentId> getMapping(IntentId intentId) {
        Versioned<Set<IntentId>> result = intentMapping.get(intentId);

        if (result != null) {
            return result.value();
        }

        return null;
    }

    @Override
    public boolean allocateMapping(IntentId keyIntentId, IntentId valIntentId) {
        Versioned<Set<IntentId>> versionedIntents = intentMapping.get(keyIntentId);

        if (versionedIntents == null) {
            Set<IntentId> newSet = new HashSet<>();
            newSet.add(valIntentId);
            intentMapping.put(keyIntentId, newSet);
        } else {
            versionedIntents.value().add(valIntentId);
        }

        return true;
    }

    @Override
    public void releaseMapping(IntentId intentId) {
        for (IntentId intent : intentMapping.keySet()) {
            // TODO: optimize by checking for identical src & dst
            Set<IntentId> mapping = intentMapping.get(intent).value();
            if (mapping.remove(intentId)) {
                return;
            }
        }
    }

}
