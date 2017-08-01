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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

import java.util.Optional;

/**
 * A class to represent an intent related event.
 */
@Beta
public class IntentEvent extends AbstractEvent<IntentEvent.Type, Intent> {

    public enum Type {
        /**
         * Signifies that an intent is to be installed or reinstalled.
         */
        INSTALL_REQ,

        /**
         * Signifies that an intent has been successfully installed.
         */
        INSTALLED,

        /**
         * Signifies that an intent has failed compilation and that it cannot
         * be satisfied by the network at this time.
         */
        FAILED,

        /**
         * Signifies that an intent will be withdrawn.
         */
        WITHDRAW_REQ,

        /**
         * Signifies that an intent has been withdrawn from the system.
         */
        WITHDRAWN,

        /**
         * Signifies that an intent has failed installation or withdrawal, but
         * still hold some or all of its resources.
         * (e.g. link reservations, flow rules on the data plane, etc.)
         */
        CORRUPT,

        /**
         * Signifies that an intent has been purged from the system.
         */
        PURGED,

        /**
         * Signifies that an intent is being reallocated.
         */
        REALLOCATING
    }

    /**
     * Creates an event of a given type and for the specified intent and the
     * current time.
     *
     * @param type   event type
     * @param intent subject intent
     * @param time   time the event created in milliseconds since start of epoch
     */
    public IntentEvent(Type type, Intent intent, long time) {
        super(type, intent, time);
    }

    /**
     * Creates an event of a given type and for the specified intent and the
     * current time.
     *
     * @param type   event type
     * @param intent subject intent
     */
    public IntentEvent(Type type, Intent intent) {
        super(type, intent);
    }

    /**
     * Creates an IntentEvent based on the state contained in the given intent
     * data. Some states are not sent as external events, and these states will
     * return null events.
     *
     * @param data the intent data to create an event for
     * @return new intent event if the state is valid, otherwise null.
     */
    public static Optional<IntentEvent> getEvent(IntentData data) {
        return getEvent(data.state(), data.intent());
    }

    /**
     * Creates an IntentEvent based on the given state and intent. Some states
     * are not sent as external events, and these states will return null events.
     *
     * @param state new state of the intent
     * @param intent intent to put in event
     * @return new intent event if the state is valid, otherwise null.
     */
    public static Optional<IntentEvent> getEvent(IntentState state, Intent intent) {
        Type type;
        switch (state) {
            case INSTALL_REQ:
                type = Type.INSTALL_REQ;
                break;
            case INSTALLED:
                type = Type.INSTALLED;
                break;
            case REALLOCATING:
                type = Type.REALLOCATING;
                break;
            case WITHDRAW_REQ:
                type = Type.WITHDRAW_REQ;
                break;
            case WITHDRAWN:
                type = Type.WITHDRAWN;
                break;
            case FAILED:
                type = Type.FAILED;
                break;
            case CORRUPT:
                type = Type.CORRUPT;
                break;
            case PURGE_REQ:
                type = Type.PURGED;
                break;

            case COMPILING:     // FALLTHROUGH
            case INSTALLING:    // FALLTHROUGH
            case RECOMPILING:   // FALLTHROUGH
            case WITHDRAWING:   // FALLTHROUGH
            default:
                return Optional.empty();
        }
        return Optional.of(new IntentEvent(type, intent));
    }

}
