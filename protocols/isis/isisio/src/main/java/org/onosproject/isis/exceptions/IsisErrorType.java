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
package org.onosproject.isis.exceptions;

/**
 * Defines all error codes and error sub codes.
 */
public final class IsisErrorType {

    //Represents an invalid ISIS message header
    public static final byte MESSAGE_HEADER_ERROR = 1;
    //Represents an invalid ISIS message body
    public static final byte ISIS_MESSAGE_ERROR = 2;
    //Message Header error sub codes
    //Represents an invalid ISIS message length
    public static final byte BAD_MESSAGE_LENGTH = 3;
    //Represents an invalid ISIS message
    public static final byte BAD_MESSAGE = 4;

    /**
     * Creates an instance of ISIS error type.
     */
    private IsisErrorType() {
    }
}