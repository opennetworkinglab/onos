/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping;

import org.onosproject.store.Store;

/**
 * Interface of a distributed store for managing mapping information.
 */
public interface MappingStore extends Store<MappingEvent, MappingStoreDelegate> {

    enum Type {

        /**
         * Signifies that mapping information should be stored in map database.
         */
        MAP_DATABASE,

        /**
         * Signifies that mapping information should be stored in map cache.
         */
        MAP_CACHE
    }
}
