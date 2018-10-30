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

package org.onosproject.drivers.bmv2.ctl;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String DEVICE_LOCK_WAITING_TIME_IN_SEC = "deviceLockWaitingTime";
    public static final int DEVICE_LOCK_WAITING_TIME_IN_SEC_DEFAULT = 60;

    public static final String NUM_CONNECTION_RETRIES = "numConnectionRetries";
    public static final int NUM_CONNECTION_RETRIES_DEFAULT = 2;

    public static final String TIME_BETWEEN_RETRIES = "timeBetweenRetries";
    public static final int TIME_BETWEEN_RETRIES_DEFAULT = 2;
}
