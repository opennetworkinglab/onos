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

package org.onosproject.net.pi.impl;

import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.net.pi.service.PiTranslatable;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationStore;
import org.onosproject.net.pi.service.PiTranslator;

import java.util.Optional;

/**
 * Abstract implementation of a PI translator backed by a PI translation store.
 *
 * @param <T> PD entity class
 * @param <E> PI entity class
 */
public abstract class AbstractPiTranslatorImpl
        <T extends PiTranslatable, E extends PiEntity>
        implements PiTranslator<T, E> {

    private final PiTranslationStore<T, E> store;

    AbstractPiTranslatorImpl(PiTranslationStore<T, E> store) {
        this.store = store;
    }

    @Override
    public void learn(PiHandle handle, PiTranslatedEntity<T, E> entity) {
        store.addOrUpdate(handle, entity);
    }

    @Override
    public Optional<PiTranslatedEntity<T, E>> lookup(PiHandle handle) {
        return Optional.ofNullable(store.get(handle));
    }

    @Override
    public void forget(PiHandle handle) {
        store.remove(handle);
    }
}
