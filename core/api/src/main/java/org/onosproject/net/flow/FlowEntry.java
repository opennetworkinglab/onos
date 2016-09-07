/*
 * Copyright 2014-present Open Networking Laboratory
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

/**
 * Represents a generalized match &amp; action pair to be applied to
 * an infrastucture device.
 */
public interface FlowEntry extends FlowRule {


    enum FlowEntryState {

        /**
         * Indicates that this rule has been submitted for addition.
         * Not necessarily in the flow table.
         */
        PENDING_ADD,

        /**
         * Rule has been added which means it is in the flow table.
         */
        ADDED,

        /**
         * Flow has been marked for removal, might still be in flow table.
         */
        PENDING_REMOVE,

        /**
         * Flow has been removed from flow table and can be purged.
         */
        REMOVED,

        /**
         * Indicates that the installation of this flow has failed.
         */
        FAILED
    }

    /**
     * Returns the flow entry state.
     *
     * @return flow entry state
     */
    FlowEntryState state();

    /**
     * Returns the number of seconds this flow rule has been applied.
     *
     * @return number of seconds
     */
    long life();

    /**
     * Returns the time this flow rule has been applied.
     *
     * @param unit time unit the result will be converted to
     * @return time in the requested {@link TimeUnit}
     */
    long life(TimeUnit unit);

    /**
     * Returns the number of packets this flow rule has matched.
     *
     * @return number of packets
     */
    long packets();

    /**
     * Returns the number of bytes this flow rule has matched.
     *
     * @return number of bytes
     */
    long bytes();

    // TODO: consider removing this attribute
    /**
     * When this flow entry was last deemed active.
     * @return epoch time of last activity
     */
    long lastSeen();

    /**
     * Indicates the error type.
     * @return an integer value of the error
     */
    int errType();

    /**
     * Indicates the error code.
     * @return an integer value of the error
     */
    int errCode();

}
