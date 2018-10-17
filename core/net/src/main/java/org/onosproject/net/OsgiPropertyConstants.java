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

    //@Property(name = "useRegionForBalanceRoles", boolValue = DEFAULT_USE_REGION_FOR_BALANCE_ROLES,
    //        label = "Use Regions for balancing roles")
    public static final String USE_REGION_FOR_BALANCE_ROLES = "useRegionForBalanceRoles";
    public static final boolean USE_REGION_FOR_BALANCE_ROLES_DEFAULT = false;

    //@Property(name = "rebalanceRolesOnUpgrade",
    //        boolValue = DEFAULT_REBALANCE_ROLES_ON_UPGRADE,
    //        label = "Automatically rebalance roles following an upgrade")
    public static final String REBALANCE_ROLES_ON_UPGRADE = "rebalanceRolesOnUpgrade";
    public static final boolean REBALANCE_ROLES_ON_UPGRADE_DEFAULT = true;

    //@Property(name = "sharedThreadPoolSize", intValue = DEFAULT_POOL_SIZE,
    //        label = "Configure shared pool maximum size ")
    public static final String SHARED_THREAD_POOL_SIZE = "sharedThreadPoolSize";
    public static final int SHARED_THREAD_POOL_SIZE_DEFAULT = 30;

    //@Property(name = "maxEventTimeLimit", intValue = DEFAULT_EVENT_TIME,
    //        label = "Maximum number of millis an event sink has to process an event")
    public static final String MAX_EVENT_TIME_LIMIT = "maxEventTimeLimit";
    public static final int MAX_EVENT_TIME_LIMIT_DEFAULT = 2000;

    //@Property(name = "sharedThreadPerformanceCheck", boolValue = DEFAULT_PERFORMANCE_CHECK,
    //        label = "Enable queue performance check on shared pool")
    public static final String CALCULATE_PERFORMANCE_CHECK = "calculatePoolPerformance";
    public static final boolean CALCULATE_PERFORMANCE_CHECK_DEFAULT = false;

    //@Property(name = "allowExtraneousRules", boolValue = ALLOW_EXTRANEOUS_RULES,
    //        label = "Allow flow rules in switch not installed by ONOS")
    public static final String ALLOW_EXTRANEOUS_RULES = "allowExtraneousRules";
    public static final boolean ALLOW_EXTRANEOUS_RULES_DEFAULT = false;

    //@Property(name = "purgeOnDisconnection", boolValue = false,
    //        label = "Purge entries associated with a device when the device goes offline")
    public static final String PURGE_ON_DISCONNECTION = "purgeOnDisconnection";
    public static final boolean PURGE_ON_DISCONNECTION_DEFAULT = false;

    //@Property(name = "fallbackFlowPollFrequency", intValue = DEFAULT_POLL_FREQUENCY,
    //        label = "Frequency (in seconds) for polling flow statistics via fallback provider")
    public static final String POLL_FREQUENCY = "fallbackFlowPollFrequency";
    public static final int POLL_FREQUENCY_DEFAULT = 30;

    //@Property(name = NUM_THREAD,
    //         intValue = DEFAULT_NUM_THREADS,
    //         label = "Number of worker threads")
    public static final String FOM_NUM_THREADS = "FOMNumThreads";
    public static final int FOM_NUM_THREADS_DEFAULT = 4;

    //@Property(name = "fallbackGroupPollFrequency", intValue = DEFAULT_POLL_FREQUENCY,
    //        label = "Frequency (in seconds) for polling groups via fallback provider")
    public static final String GM_POLL_FREQUENCY = "fallbackGroupPollFrequency";
    public static final int GM_POLL_FREQUENCY_DEFAULT = 30;

    //@Property(name = "purgeOnDisconnection", boolValue = false,
    //        label = "Purge entries associated with a device when the device goes offline")
    public static final String GM_PURGE_ON_DISCONNECTION = "purgeOnDisconnection";
    public static final boolean  GM_PURGE_ON_DISCONNECTION_DEFAULT = false;

    //@Property(name = "allowDuplicateIps", boolValue = true,
    //        label = "Enable removal of duplicate ip address")
    public static final String HM_ALLOW_DUPLICATE_IPS = "allowDuplicateIps";
    public static final boolean HM_ALLOW_DUPLICATE_IPS_DEFAULT = true;

    //@Property(name = "monitorHosts", boolValue = false,
    //        label = "Enable/Disable monitoring of hosts")
    public static final String HM_MONITOR_HOSTS = "monitorHosts";
    public static final boolean HM_MONITOR_HOSTS_DEFAULT = false;

    //@Property(name = "probeRate", longValue = 30000,
    //        label = "Set the probe Rate in milli seconds")
    public static final String HM_PROBE_RATE = "probeRate";
    public static final long HM_PROBE_RATE_DEFAULT = 30000;

    //@Property(name = "greedyLearningIpv6", boolValue = false,
    //        label = "Enable/Disable greedy learning of IPv6 link local address")
    public static final String HM_GREEDY_LEARNING_IPV6 = "greedyLearningIpv6";
    public static final boolean HM_GREEDY_LEARNING_IPV6_DEFAULT = false;

    //@Property(name = "useFlowObjectives",
    //        boolValue = DEFAULT_FLOW_OBJECTIVES,
    //        label = "Indicates whether or not to use flow objective-based compilers")
    public static final String ICR_USE_FLOW_OBJECTIVES = "useFlowObjectives";
    public static final boolean ICR_USE_FLOW_OBJECTIVES_DEFAULT = false;

    //@Property(name = "labelSelection",
    //        value = DEFAULT_LABEL_SELECTION,
    //        label = "Defines the label selection algorithm - RANDOM or FIRST_FIT")
    public static final String ICR_LABEL_SELECTION = "labelSelection";
    public static final String ICR_LABEL_SELECTION_DEFAULT = "RANDOM";

    //@Property(name = "optLabelSelection",
    //        value = DEFAULT_OPT_LABEL_SELECTION,
    //        label = "Defines the optimization for label selection algorithm - NONE, NO_SWAP, MIN_SWAP")
    public static final String ICR_OPT_LABEL_SELECTION = "optLabelSelection";
    public static final String ICR_OPT_LABEL_SELECTION_DEFAULT = "NONE";

    //@Property(name = "optimizeInstructions",
    //        boolValue = DEFAULT_FLOW_OPTIMIZATION,
    //        label = "Indicates whether or not to optimize the flows in the link collection compiler")
    public static final String ICR_FLOW_OPTIMIZATION = "optimizeInstructions";
    public static final boolean ICR_FLOW_OPTIMIZATION_DEFAULT = false;

    //@Property(name = "useCopyTtl",
    //        boolValue = DEFAULT_COPY_TTL,
    //        label = "Indicates whether or not to use copy ttl in the link collection compiler")
    public static final String ICR_COPY_TTL = "useCopyTtl";
    public static final boolean ICR_COPY_TTL_DEFAULT = false;

    //@Property(name = "enabled", boolValue = true,
    //          label = "Enables/disables the intent cleanup component")
    public static final String ICU_ENABLED = "enabled";
    public static final boolean ICU_ENABLED_DEFAULT = true;

    //@Property(name = "period", intValue = DEFAULT_PERIOD,
    //          label = "Frequency in ms between cleanup runs")
    public static final String ICU_PERIOD = "period";
    public static final int ICU_PERIOD_DEFAULT = 5; //seconds

    //@Property(name = "retryThreshold", intValue = DEFAULT_THRESHOLD,
    //        label = "Number of times to retry CORRUPT intent without delay")
    public static final String ICU_RETRY_THRESHOLD = "retryThreshold";
    public static final int ICU_RETRY_THRESHOLD_DEFAULT = 5; //tries

    //@Property(name = "nonDisruptiveInstallationWaitingTime",
    //        intValue = DEFAULT_NON_DISRUPTIVE_INSTALLATION_WAITING_TIME,
    //        label = "Number of seconds to wait during the non-disruptive installation phases")
    public static final String NON_DISRUPTIVE_INSTALLATION_WAITING_TIME =
        "nonDisruptiveInstallationWaitingTime";
    public static final int NON_DISRUPTIVE_INSTALLATION_WAITING_TIME_DEFAULT = 1;

    //@Property(name = "skipReleaseResourcesOnWithdrawal",
    //        boolValue = DEFAULT_SKIP_RELEASE_RESOURCES_ON_WITHDRAWAL,
    //        label = "Indicates whether skipping resource releases on withdrawal is enabled or not")
    public static final String IM_SKIP_RELEASE_RESOURCES_ON_WITHDRAWAL = "skipReleaseResourcesOnWithdrawal";
    public static final boolean IM_SKIP_RELEASE_RESOURCES_ON_WITHDRAWAL_DEFAULT = false;

    //@Property(name = "numThreads",
    //        intValue = DEFAULT_NUM_THREADS,
    //        label = "Number of worker threads")
    public static final String IM_NUM_THREADS = "IMNumThreads";
    public static final int IM_NUM_THREADS_DEFAULT = 12;

    //@Property(name = NUM_THREAD,
    //        intValue = DEFAULT_NUM_THREADS,
    //        label = "Number of worker threads")
    public static final String MM_NUM_THREADS = "NMNumThreads";
    public static final int MM_NUM_THREADS_DEFAULT = 12;

    //@Property(name = "fallbackMeterPollFrequency", intValue = DEFAULT_POLL_FREQUENCY,
    //        label = "Frequency (in seconds) for polling meters via fallback provider")
    public static final String MM_FALLBACK_METER_POLL_FREQUENCY = "fallbackMeterPollFrequency";
    public static final int MM_FALLBACK_METER_POLL_FREQUENCY_DEFAULT = 30;

    //@Property(name = "arpEnabled", boolValue = true,
    //        label = "Enable Address resolution protocol")
    public static final String NRM_ARP_ENABLED = "arpEnabled";
    public static final boolean NRM_ARP_ENABLED_DEFAULT = true;

    //@Property(name = "ndpEnabled", boolValue = false,
    //        label = "Enable IPv6 neighbour discovery")
    public static final String NRM_NDP_ENABLED = "ndpEnabled";
    public static final boolean NRM_NDP_ENABLED_DEFAULT = false;

    //@Property(name = "requestInterceptsEnabled", boolValue = true,
    //        label = "Enable requesting packet intercepts")
    public static final String NRM_REQUEST_INTERCEPTS_ENABLED = "requestInterceptsEnabled";
    public static final boolean NRM_REQUEST_INTERCEPTS_ENABLED_DEFAULT = true;

    //@Property(name = PROBE_INTERVAL, intValue = DEFAULT_PROBE_INTERVAL,
    //        label = "Configure interval in seconds for device pipeconf probing")
    public static final String PWM_PROBE_INTERVAL = "probeInterval";
    public static final int PWM_PROBE_INTERVAL_DEFAULT = 15;

    //@Property(name = "maxEvents", intValue = DEFAULT_MAX_EVENTS,
    //        label = "Maximum number of events to accumulate")
    public static final String DTP_MAX_EVENTS = "maxEvents";
    public static final int DTP_MAX_EVENTS_DEFAULT = 1000;

    //@Property(name = "maxIdleMs", intValue = DEFAULT_MAX_IDLE_MS,
    //        label = "Maximum number of millis between events")
    public static final String DTP_MAX_IDLE_MS = "maxIdleMs";
    public static final int DTP_MAX_IDLE_MS_DEFAULT = 10;

    //@Property(name = "maxBatchMs", intValue = DEFAULT_MAX_BATCH_MS,
    //        label = "Maximum number of millis for whole batch")
    public static final String DTP_MAX_BATCH_MS = "maxBatchMs";
    public static final int DTP_MAX_BATCH_MS_DEFAULT = 50;
}
