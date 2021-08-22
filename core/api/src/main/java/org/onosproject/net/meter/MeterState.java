/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.meter;

/**
 * Represents the state of the meter as seen by the store.
 */
public enum MeterState {

    /**
     * The meter is in the process of being added.
     */
    PENDING_ADD,

    /**
     * The meter has been added.
     */
    ADDED,

    /**
     * The meter is in the process of being removed.
     */
    PENDING_REMOVE,

}
