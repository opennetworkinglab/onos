/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pcep.controller;

/**
 * Representation of PCEP database sync status on session establishment.
 */
public enum PcepSyncStatus {

    /**
     * Specifies that the DB state is not synchronized.
     */
    NOT_SYNCED(0),

    /**
     * Specifies that the DB state is currently undergoing synchronization.
     */
    IN_SYNC(1),

    /**
     * Specifies that the DB state synchronization is completed.
     */
    SYNCED(2);

    int value;

    /**
     * Assign val with the value as the sync status.
     *
     * @param val sync status
     */
    PcepSyncStatus(int val) {
        value = val;
    }
}
