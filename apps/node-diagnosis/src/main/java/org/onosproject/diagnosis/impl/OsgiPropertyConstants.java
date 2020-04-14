/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.diagnosis.impl;

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    static final String INITIAL_POLL_DELAY_MINUTE = "initialPollDelayMinute";
    static final int DEFAULT_INITIAL_POLL_DELAY_MINUTE = 5;

    static final String POLL_FREQUENCY_MINUTE = "pollFrequencyMinute";
    static final int DEFAULT_POLL_FREQUENCY_MINUTE = 1;

    static final String REBOOT_RETRY_COUNT = "rebootRetryCount";
    static final int DEFAULT_REBOOT_RETRY_COUNT = 10;

    static final String INITIAL_CLUSTER_TIMEOUT_PERIOD = "initialClusterTimeoutPeriod";
    static final int DEFAULT_CLUSTER_TIMEOUT_PERIOD = 4;

    static final String INITIAL_DIAGNOSIS_ACTION = "initialDiagnosisAction";
    static final boolean DEFAULT_DIAGNOSIS_ACTION = true;

}
