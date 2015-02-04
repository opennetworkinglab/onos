/*
 * Copyright 2014 Open Networking Laboratory
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

import org.onosproject.event.AbstractEvent;

/**
 * A class to represent an intent related event.
 */
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
         * Signifies that an intent has failed compilation or installation.
         */
        FAILED,

        /**
         * Signifies that an intent will be withdrawn.
         */
        WITHDRAW_REQ,

        /**
         * Signifies that an intent has been withdrawn from the system.
         */
        WITHDRAWN
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

    public static IntentEvent getEvent(IntentData data) {
        return getEvent(data.state(), data.intent());
    }

    public static IntentEvent getEvent(IntentState state, Intent intent) {
        Type type;
        switch (state) {
            case INSTALL_REQ:
                type = Type.INSTALL_REQ;
                break;
            case INSTALLED:
                type = Type.INSTALLED;
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

            //fallthrough to default from here
            case COMPILING:
            case INSTALLING:
            case RECOMPILING:
            case WITHDRAWING:
            default:
                throw new IllegalArgumentException(
                        "Intent event cannot have transient state: " + state);
        }
        return new IntentEvent(type, intent);
    }

}
