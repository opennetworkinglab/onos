/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.net.intent;

import org.onlab.onos.event.AbstractEvent;

/**
 * A class to represent an intent related event.
 */
public class IntentEvent extends AbstractEvent<IntentEvent.Type, Intent> {

    public enum Type {
        /**
         * Signifies that a new intent has been submitted to the system.
         */
        SUBMITTED,

        /**
         * Signifies that an intent has been successfully installed.
         */
        INSTALLED,

        /**
         * Signifies that an intent has failed compilation or installation.
         */
        FAILED,

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

}
