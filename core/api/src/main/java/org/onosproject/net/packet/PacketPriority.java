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
package org.onosproject.net.packet;

/**
 * Priorities available to applications for requests for packets from the data
 * plane.
 */
public enum PacketPriority {
    /**
     * High priority for control traffic. This will result in all traffic
     * matching the selector being sent to the controller.
     */
    CONTROL(40000),

    /**
     * Low priority for reactive applications. Packets are only sent to the
     * controller if they fail to match any of the rules installed in the switch.
     */
    REACTIVE(5);

    private final int priorityValue;

    PacketPriority(int priorityValue) {
        this.priorityValue = priorityValue;
    }

    /**
     * Returns the integer value of the priority level.
     *
     * @return priority value
     */
    public int priorityValue() {
        return priorityValue;
    }

    public String toString() {
        return String.valueOf(priorityValue);
    }
}
