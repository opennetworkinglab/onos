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
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.store.Store;

/**
 * PI translation service store abstraction that acts as a multi-language
 * dictionary. For each pipeconf ID (language) it maintains a mapping between a
 * protocol-dependent (PD) entity and an equivalent protocol-independent (PI)
 * one.
 */
@Beta
public interface PiTranslationStore
        extends Store<PiTranslationEvent, PiTranslationStoreDelegate> {

    /**
     * Adds or update a mapping between the given PD entity (original) and the
     * translated PI counterpart, for the given pipeconf ID.
     *
     * @param original   PD entity
     * @param translated PI entity
     * @param pipeconfId pipeconf ID
     */
    void addOrUpdate(PiTranslatable original, PiEntity translated,
                     PiPipeconfId pipeconfId);

    /**
     * Removes a previously added mapping for the given PI entity and pipeconf
     * ID.
     *
     * @param piEntity   PI entity
     * @param pipeconfId pipeconf ID
     */
    void remove(PiEntity piEntity, PiPipeconfId pipeconfId);

    /**
     * Removes all previously learned mappings for the given pipeconf ID.
     *
     * @param pipeconfId pipeconf ID
     */
    void removeAll(PiPipeconfId pipeconfId);

    /**
     * Returns a PD entity for the given PI one and pipeconf ID. Returns null if
     * this store does not contain a mapping between the two for the given
     * pipeconf ID.
     *
     * @param piEntity   PI entity
     * @param pipeconfId pipeconf ID
     * @return PD entity or null
     */
    PiTranslatable lookup(PiEntity piEntity, PiPipeconfId pipeconfId);
}
