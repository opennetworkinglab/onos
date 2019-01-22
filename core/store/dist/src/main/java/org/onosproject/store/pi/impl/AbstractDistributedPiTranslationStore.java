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

import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Distributed implementation of PiTranslationStore.
 */
public abstract class AbstractDistributedPiTranslationStore
        <T extends PiTranslatable, E extends PiEntity>
        extends AbstractStore<PiTranslationEvent<T, E>, PiTranslationStoreDelegate<T, E>>
        implements PiTranslationStore<T, E> {

    private static final String MAP_NAME_TEMPLATE = "onos-pi-translated-%s-map";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private EventuallyConsistentMap<PiHandle, PiTranslatedEntity<T, E>>
            translatedEntities;

    private final EventuallyConsistentMapListener
            <PiHandle, PiTranslatedEntity<T, E>> entityMapListener =
            new InternalEntityMapListener();

    /**
     * Returns a string that identifies the map maintained by this store among
     * others that uses this abstract class.
     *
     * @return string
     */
    protected abstract String mapSimpleName();

    @Activate
    public void activate() {
        final String fullMapName = format(MAP_NAME_TEMPLATE, mapSimpleName());
        translatedEntities = storageService
                .<PiHandle, PiTranslatedEntity<T, E>>eventuallyConsistentMapBuilder()
                .withName(fullMapName)
                .withSerializer(KryoNamespaces.API)
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
    public void addOrUpdate(PiHandle handle, PiTranslatedEntity<T, E> entity) {
        checkNotNull(handle);
        checkNotNull(entity);
        checkArgument(handle.entityType().equals(entity.entityType()),
                      "Entity type must be the same for handle and translated entity");
        translatedEntities.put(handle, entity);
    }

    @Override
    public void remove(PiHandle handle) {
        checkNotNull(handle);
        translatedEntities.remove(handle);
    }

    @Override
    public PiTranslatedEntity<T, E> get(PiHandle handle) {
        checkNotNull(handle);
        return translatedEntities.get(handle);
    }

    public Iterable<PiTranslatedEntity<T, E>> getAll() {
        return translatedEntities.values();
    }

    private class InternalEntityMapListener
            implements EventuallyConsistentMapListener
                               <PiHandle, PiTranslatedEntity<T, E>> {

        @Override
        public void event(EventuallyConsistentMapEvent<PiHandle,
                PiTranslatedEntity<T, E>> event) {
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
            notifyDelegate(new PiTranslationEvent<>(type, event.value()));
        }
    }
}
