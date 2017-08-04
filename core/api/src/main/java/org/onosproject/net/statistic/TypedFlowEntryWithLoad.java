/*
 * Copyright 2015-present Open Networking Foundation
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
    private final ConnectPoint cp;
    private final TypedStoredFlowEntry tfe;
    private final Load load;

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

    /**
     * Returns current typed flow entry's polling interval.
     *
     * @param tfe typed flow entry
     * @return typed poll interval
     */
    public static long typedPollInterval(TypedStoredFlowEntry tfe) {
        checkNotNull(tfe, "TypedStoredFlowEntry cannot be null");

        PollInterval pollIntervalInstance = PollInterval.getInstance();

        switch (tfe.flowLiveType()) {
            case LONG_FLOW:
                return pollIntervalInstance.getLongPollInterval();
            case MID_FLOW:
                return pollIntervalInstance.getMidPollInterval();
            case SHORT_FLOW:
            case IMMEDIATE_FLOW:
            default:
                return pollIntervalInstance.getPollInterval();
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
        PollInterval pollIntervalInstance = PollInterval.getInstance();

        if (life < 0) {
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.UNKNOWN_FLOW);
        } else if (life < pollIntervalInstance.getPollInterval()) {
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.IMMEDIATE_FLOW);
        } else if (life < pollIntervalInstance.getMidPollInterval()) {
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.SHORT_FLOW);
        } else if (life < pollIntervalInstance.getLongPollInterval()) {
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.MID_FLOW);
        } else { // >= longPollInterval
            return new DefaultTypedFlowEntry(fe, TypedStoredFlowEntry.FlowLiveType.LONG_FLOW);
        }
    }
}
