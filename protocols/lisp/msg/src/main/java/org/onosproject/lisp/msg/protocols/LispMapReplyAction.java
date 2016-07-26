/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.msg.protocols;

/**
 * An enumeration class that represents Map-Reply action.
 *
 * The current assigned values are:
 *
 * (0) No-Action:  The map-cache is kept alive, and no packet
 * encapsulation occurs.
 *
 * (1) Natively-Forward:  The packet is not encapsulated or dropped
 *     but natively forwarded.
 *
 * (2) Send-Map-Request:  The packet invokes sending a Map-Request.
 *
 * (3) Drop:  A packet that matches this map-cache entry is dropped.
 *     An ICMP Destination Unreachable message SHOULD be sent.
 */
public enum LispMapReplyAction {

    NoAction(0),            // No-Action
    NativelyForward(1),     // Natively-Forward
    SendMapRequest(2),      // Send-Map-Request
    Drop(3);                // Drop

    private int action;

    LispMapReplyAction(int action) {
        this.action = action;
    }

    /**
     * Obtains MapReplyAction code.
     *
     * @return map reply action code
     */
    public int getAction() {
        return action;
    }

    /**
     * Obtains the LispMapReplyAction enum with given action code.
     *
     * @param action action code
     * @return an enumeration of LispMapReplyAction
     */
    public static LispMapReplyAction valueOf(int action) {
        for (LispMapReplyAction act : values()) {
            if (act.getAction() == action) {
                return act;
            }
        }
        return null;
    }
}
