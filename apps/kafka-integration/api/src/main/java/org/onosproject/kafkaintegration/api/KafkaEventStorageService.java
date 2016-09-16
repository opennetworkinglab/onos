/**
 * Copyright 2016-present Open Networking Laboratory
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.kafkaintegration.api;

import org.onosproject.kafkaintegration.api.dto.OnosEvent;

/**
 * APIs to insert and delete into a local store. This store is used to keep
 * track of events that are being published.
 */
public interface KafkaEventStorageService {

    /**
     * Inserts the Generated event into the local cache.
     *
     * @param e the ONOS Event
     * @return true if the insertion was successful
     */
    boolean insertCacheEntry(OnosEvent e);

    /**
     * Updates the counter with the most recently published event's sequence
     * number.
     *
     * @param sequenceNumber the updated value of sequence number.
     */
    void updateLastPublishedEntry(Long sequenceNumber);
}
