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

package org.onosproject.net.flow;

/**
 * Represents a flow live type for a given flow entry.
 */
public interface TypedStoredFlowEntry extends StoredFlowEntry {
    enum FlowLiveType {
        /**
         * Indicates that this rule has been submitted for addition immediately.
         * Not necessarily collecting flow stats.
         */
        IMMEDIATE_FLOW,

        /**
         * Indicates that this rule has been submitted for a short time.
         * Necessarily collecting flow stats every calAndPollInterval.
         */
        SHORT_FLOW,

        /**
         * Indicates that this rule has been submitted for a mid time.
         * Necessarily collecting flow stats every midPollInterval.
         */
        MID_FLOW,

        /**
         * Indicates that this rule has been submitted for a long time.
         * Necessarily collecting flow stats every longPollInterval.
         */
        LONG_FLOW,

        /**
         * Indicates that this rule has been submitted for UNKNOWN or ERROR.
         * Not necessarily collecting flow stats.
         */
        UNKNOWN_FLOW
    }

    /**
     * Gets the flow live type for this entry.
     *
     * @return flow live type
     */
    TypedStoredFlowEntry.FlowLiveType flowLiveType();

    /**
     * Sets the new flow live type for this entry.
     * @param liveType new flow live type.
     */
    void setFlowLiveType(TypedStoredFlowEntry.FlowLiveType liveType);
}
