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

package org.onosproject.provider.linkdiscovery.impl;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String POLL_DELAY_SECONDS = "linkPollDelaySeconds";
    public static final int POLL_DELAY_SECONDS_DEFAULT = 20;

    public static final String POLL_FREQUENCY_SECONDS = "linkPollFrequencySeconds";
    public static final int POLL_FREQUENCY_SECONDS_DEFAULT = 10;

    public static final String LINK_DISCOVERY_TIMEOUT_SECONDS = "linkDiscoveryTimeoutSeconds";
    public static final int POLL_DISCOVERY_TIMEOUT_DEFAULT = 300;

}
