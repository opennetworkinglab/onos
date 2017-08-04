/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.optical.intent.impl.compiler;

import com.google.common.annotations.Beta;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ResourceConsumerId;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

// TODO consider moving this to api bundle.
/**
 * Helper class for ResourceService related processes.
 */
@Beta
public final class ResourceHelper {

    // To avoid instantiation
    private ResourceHelper() {
    }

    /**
     * Creates IntentId object from given consumer ID.
     *
     * @param consumerId ConsumerId object
     * @return Created IntentId object.  null if failed to create or given consumer is not instance of IntentId.
     */
    public static Optional<IntentId> getIntentId(ResourceConsumerId consumerId) {
        checkNotNull(consumerId);

        if (!consumerId.isClassOf(IntentId.class)) {
            return Optional.empty();
        }

        return Optional.of(IntentId.valueOf(consumerId.value()));
    }

}
