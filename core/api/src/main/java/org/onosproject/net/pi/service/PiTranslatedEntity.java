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
import org.onosproject.net.pi.runtime.PiEntityType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of the result of a PD-to-PI translation.
 */
@Beta
public final class PiTranslatedEntity {

    private final PiTranslatable original;
    private final PiEntity translated;
    private final PiPipeconfId pipeconfId;
    private final PiEntityType type;

    public PiTranslatedEntity(PiTranslatable original, PiEntity translated,
                              PiPipeconfId pipeconfId) {
        this.original = checkNotNull(original);
        this.translated = checkNotNull(translated);
        this.pipeconfId = checkNotNull(pipeconfId);
        this.type = checkNotNull(translated.piEntityType());
    }

    /**
     * Returns the type of the translated entity.
     *
     * @return type of the translated entity
     */
    public final PiEntityType entityType() {
        return type;
    }

    /**
     * Returns the original PD entity.
     *
     * @return instance of PI translatable entity
     */
    public final PiTranslatable original() {
        return original;
    }

    /**
     * Returns the translated PI entity.
     *
     * @return PI entity
     */
    public final PiEntity translated() {
        return translated;
    }

    /**
     * The ID of the pipeconf for which this translation is valid. In other
     * words, the PI entity is guaranteed to be functionally equivalent to the
     * PD one when applied to a device configured with such pipeconf.
     *
     * @return PI pipeconf ID
     */
    public final PiPipeconfId pipeconfId() {
        return pipeconfId;
    }
}
