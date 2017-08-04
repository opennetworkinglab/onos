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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;

import java.util.Set;

@Beta
public interface IntentSetMultimap {

    /**
     * Allocates the mapping between the given intents.
     *
     * @param keyIntentId key intent ID
     * @param valIntentId value intent ID
     * @return true if mapping was successful, false otherwise
     */
    boolean allocateMapping(IntentId keyIntentId, IntentId valIntentId);

    /**
     * Returns the set of intents mapped to a lower intent.
     *
     * @param intentId intent ID
     * @return set of intent IDs
     */
    Set<IntentId> getMapping(IntentId intentId);

    /**
     * Releases the mapping of the given intent.
     *
     * @param intentId intent ID
     */
    void releaseMapping(IntentId intentId);
}
