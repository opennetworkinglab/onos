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

package org.onosproject.routing.fpm.api;

import org.onosproject.store.Store;
import org.onosproject.store.StoreDelegate;
import org.onlab.packet.IpPrefix;
import java.util.Collection;
import java.util.Optional;

/**
 * Interface to store Fpm records.
 */
public interface FpmPrefixStore extends Store<FpmPrefixStoreEvent, StoreDelegate<FpmPrefixStoreEvent>> {

    /**
     * Gets Fpm record for a prefix.
     *
     * @param prefix is the key
     * @return the Fpm record; empty if record does not exist
     */
    Optional<FpmRecord> getFpmRecord(IpPrefix prefix);

    /**
     * Gets all Fpm records from the data store.
     *
     * @return all FPM records
     */
    Collection<FpmRecord> getFpmRecords();

    /**
     * Set a delegate on the data store to be notified of events.
     *
     * @param delegate is the delegate to be added
     */
    public void setDelegate(StoreDelegate<FpmPrefixStoreEvent> delegate);

    /**
     * Unset delegate on the data store.
     *
     * @param delegate us the delegate to be removed
     */
    public void unsetDelegate(StoreDelegate<FpmPrefixStoreEvent> delegate);
}
