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

package org.onosproject.net.pi.service;

import com.google.common.annotations.Beta;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.store.Store;

/**
 * PI translation store abstraction that maintains a mapping between a PI entity
 * handle and a translated entity.
 *
 * @param <T> PD entity class (translatable to PI)
 * @param <E> PI entity class
 */
@Beta
public interface PiTranslationStore<T extends PiTranslatable, E extends PiEntity>
        extends Store<PiTranslationEvent<T, E>, PiTranslationStoreDelegate<T, E>> {

    /**
     * Adds or update a mapping between the given PI entity handle and
     * translated entity.
     *
     * @param handle PI entity handle
     * @param entity PI translated entity
     */
    void addOrUpdate(PiHandle handle, PiTranslatedEntity<T, E> entity);

    /**
     * Returns a PI translated entity for the given handle. Returns null if this
     * store does not contain a mapping between the two for the given pipeconf
     * ID.
     *
     * @param handle PI entity handle
     * @return PI translated entity
     */
    PiTranslatedEntity<T, E> get(PiHandle handle);

    /**
     * Removes a previously added mapping for the given PI entity handle.
     *
     * @param handle PI entity handle
     */
    void remove(PiHandle handle);
}
