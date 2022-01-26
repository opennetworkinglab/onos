/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;

/**
 * Abstraction of a UPF entity used to interact with the UPF-programmable device.
 */
@Beta
public interface UpfEntity {
    /**
     * Default Application ID, to be used if application filtering is performed.
     */
    byte DEFAULT_APP_ID = 0;

    /**
     * Default session index, to be used if no session metering is performed.
     */
    int DEFAULT_SESSION_INDEX = 0;

    /**
     * Default app index, to be used if no app metering is performed.
     */
    int DEFAULT_APP_INDEX = 0;

    /**
     * Returns the type of this entity.
     *
     * @return entity type
     */
    UpfEntityType type();
}
