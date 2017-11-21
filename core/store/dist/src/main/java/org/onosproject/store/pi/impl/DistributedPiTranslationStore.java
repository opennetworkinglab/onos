/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.store.pi.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.service.PiTranslatable;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationEvent;
import org.onosproject.net.pi.service.PiTranslationStore;
import org.onosproject.net.pi.service.PiTranslationStoreDelegate;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapEvent;
import org.onosproject.store.service.EventuallyConsistentMapListener;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed implementation of PiTranslationStore.
 */
@Component(immediate = true)
@Service
public class DistributedPiTranslationStore
        extends AbstractStore<PiTranslationEvent, PiTranslationStoreDelegate>
        implements PiTranslationStore {

    private static final String DIST_MAP_NAME = "onos-pi-translated-entities-map";
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private EventuallyConsistentMap<PiTranslatedEntityKey, PiTranslatedEntity>
            translatedEntities;

    private final EventuallyConsistentMapListener<PiTranslatedEntityKey,
            PiTranslatedEntity> entityMapListener = new InternalEntityMapListener();

    @Activate
    public void activate() {
        translatedEntities = storageService
                .<PiTranslatedEntityKey, PiTranslatedEntity>eventuallyConsistentMapBuilder()
                .withName(DIST_MAP_NAME)
                .withSerializer(KryoNamespace.newBuilder()
                                        .register(KryoNamespaces.API)
                                        .register(PiTranslatedEntityKey.class)
                                        .build())
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        translatedEntities.addListener(entityMapListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        translatedEntities.removeListener(entityMapListener);
        translatedEntities = null;
        log.info("Stopped");
    }

    @Override
    public void addOrUpdate(PiTranslatable original, PiEntity translated,
                            PiPipeconfId pipeconfId) {
        translatedEntities.put(PiTranslatedEntityKey.of(pipeconfId, translated),
                               new PiTranslatedEntity(original, translated, pipeconfId));
    }

    @Override
    public void remove(PiEntity piEntity, PiPipeconfId pipeconfId) {
        translatedEntities.remove(
                PiTranslatedEntityKey.of(pipeconfId, piEntity));
    }

    @Override
    public void removeAll(PiPipeconfId pipeconfId) {
        // FIXME: this can be heavy, but we assume it won't be called that often
        // How often we expect a pipeconf to be removed from the device/system?
        final Set<PiTranslatedEntityKey> keysToRemove = translatedEntities
                .keySet().parallelStream()
                .filter(k -> k.pipeconfId.equals(pipeconfId))
                .collect(Collectors.toSet());
        keysToRemove.forEach(translatedEntities::remove);
    }

    @Override
    public PiTranslatable lookup(PiEntity piEntity, PiPipeconfId pipeconfId) {
        PiTranslatedEntity translatedEntity = translatedEntities
                .get(PiTranslatedEntityKey.of(pipeconfId, piEntity));
        return translatedEntity == null ? null : translatedEntity.original();
    }


    private class InternalEntityMapListener
            implements EventuallyConsistentMapListener
                               <PiTranslatedEntityKey, PiTranslatedEntity> {

        @Override
        public void event(EventuallyConsistentMapEvent<PiTranslatedEntityKey,
                PiTranslatedEntity> event) {
            final PiTranslationEvent.Type type;
            switch (event.type()) {
                case PUT:
                    type = PiTranslationEvent.Type.LEARNED;
                    break;
                case REMOVE:
                    type = PiTranslationEvent.Type.FORGOT;
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown event type " + event.type().name());
            }
            notifyDelegate(new PiTranslationEvent(type, event.value()));
        }
    }

    /**
     * Internal representation of a key that uniquely identifies a translated
     * entity.
     */
    private static final class PiTranslatedEntityKey {

        private final PiPipeconfId pipeconfId;
        private final PiEntity piEntity;

        private PiTranslatedEntityKey(PiPipeconfId pipeconfId,
                                      PiEntity piEntity) {
            this.pipeconfId = pipeconfId;
            this.piEntity = piEntity;
        }

        public static PiTranslatedEntityKey of(PiPipeconfId pipeconfId,
                                               PiEntity piEntity) {
            return new PiTranslatedEntityKey(pipeconfId, piEntity);
        }

        public static PiTranslatedEntityKey of(PiTranslatedEntity entity) {
            return new PiTranslatedEntityKey(entity.pipeconfId(),
                                             entity.translated());
        }

        @Override
        public int hashCode() {
            return Objects.hash(pipeconfId, piEntity);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final PiTranslatedEntityKey other = (PiTranslatedEntityKey) obj;
            return Objects.equals(this.pipeconfId, other.pipeconfId)
                    && Objects.equals(this.piEntity, other.piEntity);
        }
    }
}
