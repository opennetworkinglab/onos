/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.net.flow.TypedStoredFlowEntry;
import org.onosproject.net.flow.DefaultTypedFlowEntry;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Load of flow entry of flow live type.
 */
public class TypedFlowEntryWithLoad {
    private ConnectPoint cp;
    private TypedStoredFlowEntry tfe;
    private Load load;

    //TODO: make this variables class, and share with NewAdaptivceFlowStatsCollector class
    private static final int CAL_AND_POLL_INTERVAL = 5; // means SHORT_POLL_INTERVAL
    private static final int MID_POLL_INTERVAL = 10;
    private static final int LONG_POLL_INTERVAL = 15;

    /**
     * Creates a new typed flow entry with load.
     *
     * @param cp connect point
     * @param tfe typed flow entry
     * @param load load
     */
    public TypedFlowEntryWithLoad(ConnectPoint cp, TypedStoredFlowEntry tfe, Load load) {
        this.cp = cp;
        this.tfe = tfe;
        this.load = load;
    }

    /**
     * Creates a new typed flow entry with load.
     *
     * @param cp connect point
     * @param tfe typed flow entry
     */
    public TypedFlowEntryWithLoad(ConnectPoint cp, TypedStoredFlowEntry tfe) {
        this.cp = cp;
        this.tfe = tfe;
        this.load = new DefaultLoad(tfe.bytes(), 0, typedPollInterval(tfe));
    }

    /**
     * Creates a new typed flow entry with load.
     *
     * @param cp connect point
     * @param fe flow entry
     */
    public TypedFlowEntryWithLoad(ConnectPoint cp, FlowEntry fe) {
        this.cp = cp;
        this.tfe = newTypedStoredFlowEntry(fe);
        this.load = new DefaultLoad(fe.bytes(), 0, typedPollInterval(this.tfe));
    }

    public ConnectPoint connectPoint() {
        return cp;
    }
    public TypedStoredFlowEntry typedStoredFlowEntry() {
        return tfe;
    }
    public Load load() {
        return load;
    }
    public void setLoad(Load load) {
        this.load = load;
    }

    /**
     * Returns short polling interval.
     *
     * @return short poll interval
     */
    public static int shortPollInterval() {
        return CAL_AND_POLL_INTERVAL;
    }

    /**
     * Returns mid polling interval.
     *
     * @return mid poll interval
     */
    public static int midPollInterval() {
        return MID_POLL_INTERVAL;
    }

    /**
     * Returns long polling interval.
     *
     * @return long poll interval
     */
    public static int longPollInterval() {
        return LONG_POLL_INTERVAL;
    }

    /**
     * Returns average polling interval.
     *
     * @return average poll interval
     */
    public static int avgPollInterval() {
        return (CAL_AND_POLL_INTERVAL + MID_POLL_INTERVAL + LONG_POLL_INTERVAL) / 3;
    }

    /**
     * Returns current typed flow entry's polling interval.
     *
     * @param tfe typed flow entry
     * @return typed poll interval
     */
    public static long typedPollInterval(TypedStoredFlowEntry tfe) {
        checkNotNull(tfe, "TypedStoredFlowEntry cannot be null");

        switch (tfe.flowLiveType()) {
            case LONG_FLOW:
                return LONG_POLL_INTERVAL;
            case MID_FLOW:
                return MID_POLL_INTERVAL;
            case SHORT_FLOW:
            case IMMEDIATE_FLOW:
            default:
                return CAL_AND_POLL_INTERVAL;
        }
    }

    /**
     * Creates a new typed flow entry with the given flow entry fe.
     *
     * @param fe flow entry
     * @return new typed flow entry
     */
    public static TypedStoredFlowEntry newTypedStoredFlowEntry(FlowEntry fe) {
        if (fe == null) {
            return null;
        }

        long life = fe.life();

        if (life >= LONG_POLL_INTERVAL) {
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.LONG_FLOW);
        } else if (life >= MID_POLL_INTERVAL) {
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.MID_FLOW);
        } else if (life >= CAL_AND_POLL_INTERVAL) {
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.SHORT_FLOW);
        } else if (life >= 0) {
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.IMMEDIATE_FLOW);
        } else { // life < 0
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.UNKNOWN_FLOW);
        }
    }
}
