/*
 * Copyright 2014-2016 Open Networking Laboratory
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
package org.onosproject.net.statistic;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default polling interval values.
 */
public final class PollInterval {
    private static final long DEFAULT_POLL_INTERVAL = 10;
    private static final long DEFAULT_MID_POLL_INTERVAL = 20;
    private static final long DEFAULT_LONG_POLL_INTERVAL = 30;
    private static final long DEFAULT_ENTIRE_POLL_INTERVAL = 60;

    private static PollInterval pollIntervalInstance =
            new PollInterval(DEFAULT_POLL_INTERVAL,
                             DEFAULT_MID_POLL_INTERVAL,
                             DEFAULT_LONG_POLL_INTERVAL,
                             DEFAULT_ENTIRE_POLL_INTERVAL);

    /**
     * Indicates the flow statistics poll interval in seconds.
     */
    private long pollInterval = DEFAULT_POLL_INTERVAL;
    // same as IMMEDIATE and SHORT flow live type

    // These may be used in NewFlowStatsCollector
    private long midPollInterval = DEFAULT_MID_POLL_INTERVAL; // default is 2*pollInterval
    private long longPollInterval = DEFAULT_LONG_POLL_INTERVAL; // default is 3*pollInterval
    private long entirePollInterval = DEFAULT_ENTIRE_POLL_INTERVAL; // default is 6*pollInterval

    /**
     * Returns the singleton PollInterval instance class for FlowStatisticService and other statistic services.
     * This instance is only used Adaptive Flow Sampling(adaptiveFlowSampling) mode is enabled(true).
     *
     * @return the singleton PollInterval instance class
     */
    public static PollInterval getInstance() {
        return pollIntervalInstance;
    }

    /**
     * Creates an default poll interval.
     */
    protected PollInterval() {
        this.pollInterval = DEFAULT_POLL_INTERVAL;
        this.midPollInterval = DEFAULT_MID_POLL_INTERVAL;
        this.longPollInterval = DEFAULT_LONG_POLL_INTERVAL;
        this.entirePollInterval = DEFAULT_ENTIRE_POLL_INTERVAL;
    }

    // Public construction is prohibited
    /**
     * Creates a poll interval from the parameters.
     *
     * @param pollInterval  the poll interval value
     * @param midPollInterval the mid poll interval value
     * @param longPollInterval the long poll interval value
     * @param entirePollInterval the entire poll interval value
     */
    private PollInterval(long pollInterval, long midPollInterval,
                               long longPollInterval, long entirePollInterval) {
        checkArgument(pollInterval > 0, "Poll interval must be greater than 0");
        checkArgument(midPollInterval > 0 && midPollInterval > pollInterval,
                      "Mid poll interval must be greater than 0 and pollInterval");
        checkArgument(longPollInterval > 0 && longPollInterval > midPollInterval,
                      "Long poll interval must be greater than 0 and midPollInterval");
        checkArgument(entirePollInterval > 0 && entirePollInterval > longPollInterval,
                      "Entire poll interval must be greater than 0 and longPollInterval");

        this.pollInterval = pollInterval;
        this.midPollInterval = midPollInterval;
        this.longPollInterval = longPollInterval;
        this.entirePollInterval = entirePollInterval;
    }

    /**
     * Sets the poll interval in seconds. Used solely for the purpose of
     * computing the load.
     *
     * @param newPollInterval poll interval duration in seconds
     */
    public void setPollInterval(long newPollInterval) {
        checkArgument(newPollInterval > 0, "Poll interval must be greater than 0");

        pollInterval = newPollInterval;
    }

    /**
     * Sets the mid poll interval in seconds. Used solely for the purpose of
     * computing the load.
     *
     * @param newPollInterval poll interval duration in seconds
     */
    public void setMidPollInterval(long newPollInterval) {
        checkArgument(newPollInterval > 0 && newPollInterval > pollInterval,
                      "Mid poll interval must be greater than 0 and pollInterval");

        midPollInterval = newPollInterval;
    }

    /**
     * Sets the long poll interval in seconds. Used solely for the purpose of
     * computing the load.
     *
     * @param newPollInterval poll interval duration in seconds
     */
    public void setLongPollInterval(long newPollInterval) {
        checkArgument(newPollInterval > 0 && newPollInterval > midPollInterval,
                      "Long poll interval must be greater than 0 and midPollInterval");

        longPollInterval = newPollInterval;
    }

    /**
     * Sets the entire poll interval in seconds. Used solely for the purpose of
     * computing the load.
     *
     * @param newPollInterval poll interval duration in seconds
     */
    public void setEntirePollInterval(long newPollInterval) {
        checkArgument(newPollInterval > 0 && newPollInterval > longPollInterval,
                      "Entire poll interval must be greater than 0 and longPollInterval");

        entirePollInterval = newPollInterval;
    }

    /**
     * Returns default poll interval value in seconds.
     *
     * @return default poll interval
     */
    public long getPollInterval() {
        return pollInterval;
    }

    /**
     * Returns mid poll interval value in seconds.
     *
     * @return mid poll interval
     */
    public long getMidPollInterval() {
        return midPollInterval;
    }

    /**
     * Returns long poll interval value in seconds.
     *
     * @return long poll interval
     */
    public long getLongPollInterval() {
        return longPollInterval;
    }

    /**
     * Returns entire poll interval value in seconds.
     *
     * @return entire poll interval
     */
    public long getEntirePollInterval() {
        return entirePollInterval;
    }

    /**
     * Returns average poll interval value in seconds.
     *
     * @return average poll interval
     */
    public long getAvgPollInterval() {
        return (pollInterval + midPollInterval + longPollInterval) / 3;
    }
}
