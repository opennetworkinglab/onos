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
package org.onosproject.cpman.message;

import org.onosproject.cpman.ControlMessage;
import org.onosproject.event.AbstractEvent;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Describes control message event.
 */
public class ControlMessageEvent
        extends AbstractEvent<ControlMessageEvent.Type, Set<ControlMessage>> {

    /**
     * Type of control message events.
     */
    public enum Type {
        /**
         * signifies that the control message stats has been updated.
         */
        STATS_UPDATE
    }

    /**
     * Creates an event of given type and the current time.
     *
     * @param type control message event type
     * @param controlMessages event control message subject
     */
    public ControlMessageEvent(Type type, Set<ControlMessage> controlMessages) {
        super(type, controlMessages);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type())
                .add("subject", subject())
                .toString();
    }
}
