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

package org.onosproject.faultmanagement.impl;

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    static final String POLL_FREQUENCY_SECONDS = "alarmPollFrequencySeconds";
    static final int POLL_FREQUENCY_SECONDS_DEFAULT = 60;

    static final String CLEAR_FREQUENCY_SECONDS = "clearedAlarmPurgeFrequencySeconds";
    static final int CLEAR_FREQUENCY_SECONDS_DEFAULT = 500;
}
