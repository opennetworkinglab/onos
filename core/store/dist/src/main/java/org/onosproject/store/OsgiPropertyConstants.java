/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.store;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String MESSAGE_HANDLER_THREAD_POOL_SIZE = "msgHandlerPoolSize";
    public static final int MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT = 8;

    public static final String BACKUP_PERIOD_MILLIS = "backupPeriod";
    public static final int BACKUP_PERIOD_MILLIS_DEFAULT = 2000;

    public static final String ANTI_ENTROPY_PERIOD_MILLIS = "antiEntropyPeriod";
    public static final int ANTI_ENTROPY_PERIOD_MILLIS_DEFAULT = 5000;

    public static final String EC_FLOW_RULE_STORE_PERSISTENCE_ENABLED = "persistenceEnabled";
    public static final boolean EC_FLOW_RULE_STORE_PERSISTENCE_ENABLED_DEFAULT = false;

    public static final String MAX_BACKUP_COUNT = "backupCount";
    public static final int MAX_BACKUP_COUNT_DEFAULT = 2;

    public static final String ELECTION_TIMEOUT_MILLIS = "electionTimeoutMillis";
    public static final long ELECTION_TIMEOUT_MILLIS_DEFAULT = 2500;

    public static final String GARBAGE_COLLECT = "garbageCollect";
    public static final boolean GARBAGE_COLLECT_DEFAULT = false;

    public static final String GARBAGE_COLLECT_THRESH = "gcThresh";
    public static final int GARBAGE_COLLECT_THRESH_DEFAULT = 6;

    public static final String ALLOW_EXTRANEOUS_GROUPS = "allowExtraneousGroups";
    public static final boolean ALLOW_EXTRANEOUS_GROUPS_DEFAULT = false;

    public static final String GIS_PERSISTENCE_ENABLED = "persistenceEnabled";
    public static final boolean GIS_PERSISTENCE_ENABLED_DEFAULT = false;

    public static final String DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE = "messageHandlerThreadPoolSize";
    public static final int DPS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT = 4;

    public static final String DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE = "messageHandlerThreadPoolSize";
    public static final int DFS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT = 4;

    public static final String DSS_MESSAGE_HANDLER_THREAD_POOL_SIZE = "messageHandlerThreadPoolSize";
    public static final int DSS_MESSAGE_HANDLER_THREAD_POOL_SIZE_DEFAULT = 4;

    public static final String LINK_WEIGHT_FUNCTION = "linkWeightFunction";
    public static final String LINK_WEIGHT_FUNCTION_DEFAULT = "hopCount";

    public static final String MAX_PATHS = "maxPaths";
    public static final int MAX_PATHS_DEFAULT = -1;
}
