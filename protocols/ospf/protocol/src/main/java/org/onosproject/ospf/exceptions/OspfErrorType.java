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
package org.onosproject.ospf.exceptions;

/**
 * Defines all error codes and error sub codes.
 */
public final class OspfErrorType {

    //Represents an invalid OSPF message header
    public static final byte MESSAGE_HEADER_ERROR = 1;
    //Represents an invalid OSPF message body
    public static final byte OSPF_MESSAGE_ERROR = 2;
    //Message Header error sub codes
    //Represents an invalid OSPF message length
    public static final byte BAD_MESSAGE_LENGTH = 2;
    //Represents an invalid OSPF message
    public static final byte BAD_MESSAGE = 4;

    /**
     * Creates an instance of OSPF error type.
     */
    private OspfErrorType() {
    }
}