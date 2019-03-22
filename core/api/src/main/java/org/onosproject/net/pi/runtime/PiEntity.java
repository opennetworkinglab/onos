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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import org.onosproject.net.DeviceId;

/**
 * Abstraction of a runtime entity of a protocol-independent pipeline.
 */
@Beta
public interface PiEntity {

    /**
     * Returns the type of this entity.
     *
     * @return entity type
     */
    PiEntityType piEntityType();

    /**
     * Returns a handle for this PI entity and the given device ID.
     *
     * @param deviceId device ID
     * @return handle
     */
    PiHandle handle(DeviceId deviceId);
}
