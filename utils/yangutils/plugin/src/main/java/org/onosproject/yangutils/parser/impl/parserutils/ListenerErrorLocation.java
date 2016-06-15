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

package org.onosproject.yangutils.parser.impl.parserutils;

/**
 * Represents listener error location.
 */
public enum ListenerErrorLocation {
    /**
     * Represents that the error location is before processing.
     */
    ENTRY(),

    /**
     * Represents that the error location is before processing.
     */
    EXIT();

    /**
     * Returns the message corresponding to listener error location.
     *
     * @param errorLocation enum value for type of error
     * @return message corresponding to listener error location
     */
    public static String getErrorLocationMessage(ListenerErrorLocation errorLocation) {

        switch (errorLocation) {
            case ENTRY:
                return "before";
            case EXIT:
                return "after";
            default:
                return "during";
        }
    }
}