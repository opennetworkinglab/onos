/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.net.flow;

import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Default flow entry class with FlowLiveType value, IMMEDIATE_FLOW, SHORT_FLOW, MID_FLOW, LONG_FLOW.
 */
public class DefaultTypedFlowEntry extends DefaultFlowEntry
    implements TypedStoredFlowEntry {
    private FlowLiveType liveType;


    /**
     * Creates a typed flow entry from flow rule and its statistics, with default flow live type(IMMEDIATE_FLOW).
     *
     * @param rule the flow rule
     * @param state the flow state
     * @param life the flow duration since creation
     * @param lifeTimeUnit the time unit of life
     * @param packets the flow packets count
     * @param bytes the flow bytes count
     */
    public DefaultTypedFlowEntry(FlowRule rule, FlowEntryState state,
                                 long life, TimeUnit lifeTimeUnit, long packets, long bytes) {
        super(rule, state, life, lifeTimeUnit, packets, bytes);
        this.liveType = FlowLiveType.IMMEDIATE_FLOW;
    }

    /**
     * Creates a typed flow entry from flow rule and its statistics, with default flow live type(IMMEDIATE_FLOW).
     *
     * @param rule the flow rule
     * @param state the flow state
     * @param life the flow duration since creation
     * @param packets the flow packets count
     * @param bytes the flow bytes count
     *
     */
    public DefaultTypedFlowEntry(FlowRule rule, FlowEntryState state,
                            long life, long packets, long bytes) {
        super(rule, state, life, packets, bytes);
        this.liveType = FlowLiveType.IMMEDIATE_FLOW;
    }

    /**
     * Creates a typed flow entry from flow rule,  with default flow live type(IMMEDIATE_FLOW).
     *
     * @param rule the flow rule
     *
     */
    public DefaultTypedFlowEntry(FlowRule rule) {
        super(rule);
        this.liveType = FlowLiveType.IMMEDIATE_FLOW;
    }

    /**
     * Creates a typed flow entry from flow entry,  with default flow live type(IMMEDIATE_FLOW).
     *
     * @param fe the flow entry
     *
     */
    public DefaultTypedFlowEntry(FlowEntry fe) {
        super(fe, fe.state(), fe.life(NANOSECONDS), NANOSECONDS, fe.packets(), fe.bytes());
        this.liveType = FlowLiveType.IMMEDIATE_FLOW;
    }

    /**
     * Creates a typed flow entry from flow rule and flow live type.
     *
     * @param rule the flow rule
     * @param liveType the flow live type
     *
     */
    public DefaultTypedFlowEntry(FlowRule rule, FlowLiveType liveType) {
        super(rule);
        this.liveType = liveType;
    }

    /**
     * Creates a typed flow entry from flow entry and flow live type.
     *
     * @param fe the flow rule
     * @param liveType the flow live type
     *
     */
    public DefaultTypedFlowEntry(FlowEntry fe,  FlowLiveType liveType) {
        super(fe, fe.state(), fe.life(NANOSECONDS), NANOSECONDS, fe.packets(), fe.bytes());
        this.liveType = liveType;
    }

    /**
     * Creates a typed flow entry from flow rule, error code and flow live type.
     *
     * @param rule the flow rule
     * @param errType the flow error type
     * @param errCode the flow error code
     * @param liveType the flow live type
     *
     */
    public DefaultTypedFlowEntry(FlowRule rule, int errType, int errCode, FlowLiveType liveType) {
        super(rule, errType, errCode);
        this.liveType = liveType;
    }

    @Override
    public FlowLiveType flowLiveType() {
        return this.liveType;
    }

    @Override
    public void setFlowLiveType(FlowLiveType liveType) {
        this.liveType = liveType;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("entry", super.toString())
                .add("type", liveType)
                .toString();
    }
}

