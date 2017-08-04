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
package org.onosproject.net.flow;


import java.util.concurrent.TimeUnit;

public interface StoredFlowEntry extends FlowEntry {

    /**
     * Sets the last active epoch time.
     */
    void setLastSeen();

    /**
     * Sets the new state for this entry.
     * @param newState new flow entry state.
     */
    void setState(FlowEntryState newState);

    /**
     * Sets how long this entry has been entered in the system.
     * @param lifeSecs seconds
     */
    void setLife(long lifeSecs);

    /**
     * Sets how long this entry has been entered in the system.
     * @param life time
     * @param timeUnit unit of time
     */
    void setLife(long life, TimeUnit timeUnit);

    /**
     * Sets the flow live type,
     * i.e., IMMEDIATE, SHORT, MID, LONG.
     * @param liveType flow live type
     */
    void setLiveType(FlowLiveType liveType);

    /**
     * Number of packets seen by this entry.
     * @param packets a long value
     */
    void setPackets(long packets);

    /**
     * Number of bytes seen by this rule.
     * @param bytes a long value
     */
    void setBytes(long bytes);

}
