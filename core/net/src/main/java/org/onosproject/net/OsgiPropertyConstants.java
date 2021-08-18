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

package org.onosproject.net;

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String USE_REGION_FOR_BALANCE_ROLES = "useRegionForBalanceRoles";
    public static final boolean USE_REGION_FOR_BALANCE_ROLES_DEFAULT = false;

    public static final String REBALANCE_ROLES_ON_UPGRADE = "rebalanceRolesOnUpgrade";
    public static final boolean REBALANCE_ROLES_ON_UPGRADE_DEFAULT = true;

    public static final String SHARED_THREAD_POOL_SIZE = "sharedThreadPoolSize";
    public static final int SHARED_THREAD_POOL_SIZE_DEFAULT = 30;

    public static final String MAX_EVENT_TIME_LIMIT = "maxEventTimeLimit";
    public static final int MAX_EVENT_TIME_LIMIT_DEFAULT = 2000;

    public static final String CALCULATE_PERFORMANCE_CHECK = "sharedThreadPerformanceCheck";
    public static final boolean CALCULATE_PERFORMANCE_CHECK_DEFAULT = false;

    public static final String ALLOW_EXTRANEOUS_RULES = "allowExtraneousRules";
    public static final boolean ALLOW_EXTRANEOUS_RULES_DEFAULT = false;

    public static final String IMPORT_EXTRANEOUS_RULES = "importExtraneousRules";
    public static final boolean IMPORT_EXTRANEOUS_RULES_DEFAULT = false;

    public static final String PURGE_ON_DISCONNECTION = "purgeOnDisconnection";
    public static final boolean PURGE_ON_DISCONNECTION_DEFAULT = false;

    public static final String POLL_FREQUENCY = "fallbackFlowPollFrequency";
    public static final int POLL_FREQUENCY_DEFAULT = 30;

    public static final String FOM_NUM_THREADS = "numThreads";
    public static final int FOM_NUM_THREADS_DEFAULT = 4;

    public static final String GM_POLL_FREQUENCY = "fallbackGroupPollFrequency";
    public static final int GM_POLL_FREQUENCY_DEFAULT = 30;

    public static final String GM_PURGE_ON_DISCONNECTION = "purgeOnDisconnection";
    public static final boolean  GM_PURGE_ON_DISCONNECTION_DEFAULT = false;

    public static final String HM_ALLOW_DUPLICATE_IPS = "allowDuplicateIps";
    public static final boolean HM_ALLOW_DUPLICATE_IPS_DEFAULT = true;

    public static final String HM_MONITOR_HOSTS = "monitorHosts";
    public static final boolean HM_MONITOR_HOSTS_DEFAULT = false;

    public static final String HM_PROBE_RATE = "probeRate";
    public static final long HM_PROBE_RATE_DEFAULT = 30000;

    public static final String HM_HOST_MOVE_TRACKER_ENABLE = "hostMoveTrackerEnabled";
    public static final boolean HM_HOST_MOVE_TRACKER_ENABLE_DEFAULT = false;

    public static final String HM_HOST_MOVED_THRESHOLD_IN_MILLIS = "hostMoveThresholdInMillis";
    public static final int HM_HOST_MOVED_THRESHOLD_IN_MILLIS_DEFAULT = 200000;

    public static final String HM_HOST_MOVE_COUNTER = "hostMoveCounter";
    public static final int HM_HOST_MOVE_COUNTER_DEFAULT = 3;

    public static final String HM_OFFENDING_HOST_EXPIRY_IN_MINS = "offendingHostExpiryInMins";
    public static final long HM_OFFENDING_HOST_EXPIRY_IN_MINS_DEFAULT = 1;

    public static final String HM_OFFENDING_HOST_THREADS_POOL_SIZE = "offendingHostClearThreadPool";
    public static final int HM_OFFENDING_HOST_THREADS_POOL_SIZE_DEFAULT = 10;

    public static final String HM_GREEDY_LEARNING_IPV6 = "greedyLearningIpv6";
    public static final boolean HM_GREEDY_LEARNING_IPV6_DEFAULT = false;

    public static final String ICR_USE_FLOW_OBJECTIVES = "useFlowObjectives";
    public static final boolean ICR_USE_FLOW_OBJECTIVES_DEFAULT = false;

    public static final String ICR_LABEL_SELECTION = "labelSelection";
    public static final String ICR_LABEL_SELECTION_DEFAULT = "RANDOM";

    public static final String ICR_OPT_LABEL_SELECTION = "optLabelSelection";
    public static final String ICR_OPT_LABEL_SELECTION_DEFAULT = "NONE";

    public static final String ICR_FLOW_OPTIMIZATION = "optimizeInstructions";
    public static final boolean ICR_FLOW_OPTIMIZATION_DEFAULT = false;

    public static final String ICR_COPY_TTL = "useCopyTtl";
    public static final boolean ICR_COPY_TTL_DEFAULT = false;

    public static final String ICU_ENABLED = "enabled";
    public static final boolean ICU_ENABLED_DEFAULT = true;

    public static final String ICU_PERIOD = "period";
    public static final int ICU_PERIOD_DEFAULT = 5; //seconds

    public static final String ICU_RETRY_THRESHOLD = "retryThreshold";
    public static final int ICU_RETRY_THRESHOLD_DEFAULT = 5; //tries

    public static final String NON_DISRUPTIVE_INSTALLATION_WAITING_TIME = "nonDisruptiveInstallationWaitingTime";
    public static final int NON_DISRUPTIVE_INSTALLATION_WAITING_TIME_DEFAULT = 1;

    public static final String IM_SKIP_RELEASE_RESOURCES_ON_WITHDRAWAL = "skipReleaseResourcesOnWithdrawal";
    public static final boolean IM_SKIP_RELEASE_RESOURCES_ON_WITHDRAWAL_DEFAULT = false;

    public static final String IM_NUM_THREADS = "numThreads";
    public static final int IM_NUM_THREADS_DEFAULT = 12;

    public static final String MM_NUM_THREADS = "numThreads";
    public static final int MM_NUM_THREADS_DEFAULT = 12;

    public static final String MM_FALLBACK_METER_POLL_FREQUENCY = "fallbackMeterPollFrequency";
    public static final int MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT = 30;

    public static final String MM_PURGE_ON_DISCONNECTION = "purgeOnDisconnection";
    public static final boolean MM_PURGE_ON_DISCONNECTION_DEFAULT = false;

    public static final String MM_USER_DEFINED_INDEX = "userDefinedIndex";
    public static final boolean MM_USER_DEFINED_INDEX_DEFAULT = false;

    public static final String NRM_ARP_ENABLED = "arpEnabled";
    public static final boolean NRM_ARP_ENABLED_DEFAULT = true;

    public static final String NRM_NDP_ENABLED = "ndpEnabled";
    public static final boolean NRM_NDP_ENABLED_DEFAULT = false;

    public static final String NRM_REQUEST_INTERCEPTS_ENABLED = "requestInterceptsEnabled";
    public static final boolean NRM_REQUEST_INTERCEPTS_ENABLED_DEFAULT = true;

    public static final String PWM_PROBE_INTERVAL = "probeInterval";
    public static final int PWM_PROBE_INTERVAL_DEFAULT = 15;

    public static final String DTP_MAX_EVENTS = "maxEvents";
    public static final int DTP_MAX_EVENTS_DEFAULT = 1000;

    public static final String DTP_MAX_IDLE_MS = "maxIdleMs";
    public static final int DTP_MAX_IDLE_MS_DEFAULT = 10;

    public static final String DTP_MAX_BATCH_MS = "maxBatchMs";
    public static final int DTP_MAX_BATCH_MS_DEFAULT = 50;

    public static final String AUDIT_ENABLED = "auditEnabled";
    public static final boolean AUDIT_ENABLED_DEFAULT = false;

    public static final String AUDIT_LOGGER = "auditLogger";
    public static final String AUDIT_LOGGER_DEFAULT = "securityAudit";

    public static final String FOM_ACCUMULATOR_MAX_OBJECTIVES = "accumulatorMaxObjectives";
    public static final int FOM_ACCUMULATOR_MAX_OBJECTIVES_DEFAULT = 1000;

    public static final String FOM_ACCUMULATOR_MAX_IDLE_MILLIS = "accumulatorMaxIdleMillis";
    public static final int FOM_ACCUMULATOR_MAX_IDLE_MILLIS_DEFAULT = 10;

    public static final String FOM_ACCUMULATOR_MAX_BATCH_MILLIS = "accumulatorMaxBatchMillis";
    public static final int FOM_ACCUMULATOR_MAX_BATCH_MILLIS_DEFAULT = 500;

    public static final String IFOM_OBJ_TIMEOUT_MS = "objectiveTimeoutMs";
    public static final int IFOM_OBJ_TIMEOUT_MS_DEFAULT = 15000;
}
