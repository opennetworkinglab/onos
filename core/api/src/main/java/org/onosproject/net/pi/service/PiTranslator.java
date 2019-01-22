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
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.net.pi.runtime.PiHandle;

import java.util.Optional;

/**
 * A translator of PI entities to equivalent PD ones which offer means to learn
 * translated entities for later use.
 *
 * @param <T> PD entity class (translatable to PI)
 * @param <E> PI entity class
 */
@Beta
public interface PiTranslator<T extends PiTranslatable, E extends PiEntity> {

    /**
     * Translate the given PD entity (original) and returns a PI entity that is
     * equivalent to he PD one for the given pipeconf.
     *
     * @param original PD entity
     * @param pipeconf pipeconf
     * @return PI entity
     * @throws PiTranslationException if a translation is not possible (see
     *                                message for an explanation)
     */
    E translate(T original, PiPipeconf pipeconf)
            throws PiTranslationException;

    /**
     * Stores a mapping between the given translated entity and handle.
     *
     * @param handle PI entity handle
     * @param entity PI translated entity
     */
    void learn(PiHandle handle, PiTranslatedEntity<T, E> entity);

    /**
     * Returns a PI translated entity that was previously associated with the
     * given  handle, if present. If not present, it means a mapping between the
     * two has not been learned by the system (via {@link #learn(PiHandle,
     * PiTranslatedEntity)}) or that it has been removed (via {@link
     * #forget(PiHandle)}). the
     *
     * @param handle PI entity handle
     * @return optional PI translated entity
     */
    Optional<PiTranslatedEntity<T, E>> lookup(PiHandle handle);

    /**
     * Removes any mapping for the given PI entity handle.
     *
     * @param handle PI entity handle.
     */
    void forget(PiHandle handle);
}
