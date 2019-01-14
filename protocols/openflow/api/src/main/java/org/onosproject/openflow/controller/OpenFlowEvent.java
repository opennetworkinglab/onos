/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openflow.controller;

import org.onosproject.event.AbstractEvent;

/**
 * Represent OpenFlow Classifiers events.
 */
public class OpenFlowEvent extends AbstractEvent<OpenFlowEvent.Type, OpenFlowClassifier> {

    /**
     * Enum of OpenFlow event type.
     */
    public enum Type {
        /**
         * Signifies that a new packet classifier has been added.
         */
        INSERT,
        /**
         * Signifies that a packet classifier has been removed.
         */
        REMOVE
    }

    /**
     * Constructor.
     *
     * @param type the OpenFlow event type
     * @param subject the OpenFlow update data
     */
    public OpenFlowEvent(Type type, OpenFlowClassifier subject) {
        super(type, subject);
    }
}
