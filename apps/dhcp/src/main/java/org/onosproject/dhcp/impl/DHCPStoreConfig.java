/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.dhcp.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.basics.BasicElementConfig;

/**
 * DHCP Store Config class.
 */
public class DHCPStoreConfig extends Config<ApplicationId> {

    public static final String TIMER_DELAY = "delay";
    public static final String DEFAULT_TIMEOUT = "timeout";
    public static final String START_IP = "startip";
    public static final String END_IP = "endip";

    /**
     * Returns the delay after which the dhcp server will purge expired entries.
     *
     * @return time delay or null if not set
     */
    public String timerDelay() {
        return get(TIMER_DELAY, null);
    }

    /**
     * Sets the delay after which the dhcp server will purge expired entries.
     *
     * @param delay new time delay; null to clear
     * @return self
     */
    public BasicElementConfig timerDelay(String delay) {
        return (BasicElementConfig) setOrClear(TIMER_DELAY, delay);
    }

    /**
     * Returns the default timeout for pending assignments.
     *
     * @return default timeout or null if not set
     */
    public String defaultTimeout() {
        return get(DEFAULT_TIMEOUT, null);
    }

    /**
     * Sets the default timeout for pending assignments.
     *
     * @param defaultTimeout new default timeout; null to clear
     * @return self
     */
    public BasicElementConfig defaultTimeout(String defaultTimeout) {
        return (BasicElementConfig) setOrClear(DEFAULT_TIMEOUT, defaultTimeout);
    }

    /**
     * Returns the start IP for the available IP Range.
     *
     * @return start IP or null if not set
     */
    public String startIP() {
        return get(START_IP, null);
    }

    /**
     * Sets the start IP for the available IP Range.
     *
     * @param startIP new start IP; null to clear
     * @return self
     */
    public BasicElementConfig startIP(String startIP) {
        return (BasicElementConfig) setOrClear(START_IP, startIP);
    }

    /**
     * Returns the end IP for the available IP Range.
     *
     * @return end IP or null if not set
     */
    public String endIP() {
        return get(END_IP, null);
    }

    /**
     * Sets the end IP for the available IP Range.
     *
     * @param endIP new end IP; null to clear
     * @return self
     */
    public BasicElementConfig endIP(String endIP) {
        return (BasicElementConfig) setOrClear(END_IP, endIP);
    }
}
