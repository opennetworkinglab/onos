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
import org.onosproject.net.pi.runtime.PiEntityType;
import org.onosproject.net.pi.runtime.PiHandle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of the result of a PD-to-PI translation associated to a PI
 * entity handle.
 */
@Beta
public final class PiTranslatedEntity<T extends PiTranslatable, E extends PiEntity> {

    private final T original;
    private final E translated;
    private final PiHandle handle;

    /**
     * Creates a new translated entity.
     *
     * @param original PD entity
     * @param translated PI entity
     * @param handle PI entity handle
     */
    public PiTranslatedEntity(T original, E translated, PiHandle handle) {
        this.original = checkNotNull(original);
        this.translated = checkNotNull(translated);
        this.handle = checkNotNull(handle);
    }

    /**
     * Returns the type of the translated entity.
     *
     * @return type of the translated entity
     */
    public final PiEntityType entityType() {
        return translated.piEntityType();
    }

    /**
     * Returns the original PD entity.
     *
     * @return instance of PI translatable entity
     */
    public final T original() {
        return original;
    }

    /**
     * Returns the translated PI entity.
     *
     * @return PI entity
     */
    public final E translated() {
        return translated;
    }

    /**
     * Returns the PI entity handle.
     *
     * @return PI entity handle
     */
    public final PiHandle handle() {
        return handle;
    }
}
