/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.StoredFlowEntry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Load of flow entry with flow live type.
 */
public class FlowEntryWithLoad {
    private final ConnectPoint cp;
    private final FlowEntry fe;
    private final Load load;

    /**
     * Creates a new flow entry with load.
     *
     * @param cp connect point
     * @param fe flow entry with live type
     * @param load load
     */
    public FlowEntryWithLoad(ConnectPoint cp, FlowEntry fe, Load load) {
        checkArgument(fe instanceof StoredFlowEntry, "FlowEntry must be StoredFlowEntry class type");
        this.cp = cp;
        this.fe = fe;
        this.load = load;
    }

    /**
     * Creates a new flow entry with load.
     *
     * @param cp connect point
     * @param fe flow entry with live type
     */
    public FlowEntryWithLoad(ConnectPoint cp, FlowEntry fe) {
        checkArgument(fe instanceof StoredFlowEntry, "FlowEntry must be StoredFlowEntry class type");
        this.cp = cp;
        this.fe = fe;
        this.load = new DefaultLoad(fe.bytes(), 0, typedPollInterval(fe));
    }

    /**
     * Returns connect point.
     *
     * @return connect point
     */
    public ConnectPoint connectPoint() {
        return cp;
    }

    /**
     * Returns stored flow entry.
     *
     * @return flow entry
     */
    public StoredFlowEntry storedFlowEntry() {
        return (StoredFlowEntry) fe;
    }

    /**
     * Returns current load.
     *
     * @return load
     */
    public Load load() {
        return load;
    }

    /**
     * Returns current flow entry's polling interval.
     *
     * @param fe flow entry
     * @return poll interval time unit in seconds
     */
    private long typedPollInterval(FlowEntry fe) {
        checkNotNull(fe, "FlowEntry cannot be null");

        PollInterval pollIntervalInstance = PollInterval.getInstance();

        switch (fe.liveType()) {
            case LONG:
                return pollIntervalInstance.getLongPollInterval();
            case MID:
                return pollIntervalInstance.getMidPollInterval();
            case SHORT:
            case IMMEDIATE:
            case UNKNOWN:
            default:
                return pollIntervalInstance.getPollInterval();
        }
    }
}
