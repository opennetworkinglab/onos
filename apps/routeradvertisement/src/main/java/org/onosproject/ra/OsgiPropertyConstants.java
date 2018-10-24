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

package org.onosproject.ra;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    public static final String RA_THREADS_POOL = "raPoolSize";
    public static final int RA_THREADS_POOL_SIZE_DEFAULT = 10;

    public static final String RA_THREADS_DELAY = "raThreadDelay";
    public static final int RA_THREADS_DELAY_DEFAULT = 5;

    public static final String RA_FLAG_MBIT_STATUS = "raFlagMbitStatus";
    public static final boolean RA_FLAG_MBIT_STATUS_DEFAULT = false;

    public static final String RA_FLAG_OBIT_STATUS = "raFlagObitStatus";
    public static final boolean RA_FLAG_OBIT_STATUS_DEFAULT = false;

    public static final String RA_OPTION_PREFIX_STATUS = "raOptionPrefixStatus";
    public static final boolean RA_OPTION_PREFIX_STATUS_DEFAULT = false;

    public static final String RA_GLOBAL_PREFIX_CONF_STATUS = "raGlobalPrefixConfStatus";
    public static final boolean RA_GLOBAL_PREFIX_CONF_STATUS_DEFAULT = true;

}
