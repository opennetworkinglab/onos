/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgpio.types;

/**
 * BgpErrorType class defines all errorCodes and error Subcodes required for Notification message.
 */
public final class BGPErrorType {
    private BGPErrorType() {
    }

    //Error Codes
    public static final byte MESSAGE_HEADER_ERROR = 1;
    public static final byte OPEN_MESSAGE_ERROR = 2;
    public static final byte UPDATE_MESSAGE_ERROR = 3;
    public static final byte HOLD_TIMER_EXPIRED = 4;
    public static final byte FINITE_STATE_MACHINE_ERROR = 4;
    public static final byte CEASE = 5;

    //Message Header Error subcodes
    public static final byte CONNECTION_NOT_SYNCHRONIZED = 1;
    public static final byte BAD_MESSAGE_LENGTH = 2;
    public static final byte BAD_MESSAGE_TYPE = 3;

    //OPEN Message Error subcodes
    public static final byte UNSUPPORTED_VERSION_NUMBER = 1;
    public static final byte BAD_PEER_AS = 2;
    public static final byte BAD_BGP_IDENTIFIER = 3;
    public static final byte UNSUPPORTED_OPTIONAL_PARAMETER = 4;
    public static final byte UNACCEPTABLE_HOLD_TIME = 5;

    //UPDATE Message Error subcodes
    public static final byte MALFORMED_ATTRIBUTE_LIST = 1;
    public static final byte UNRECOGNIZED_WELLKNOWN_ATTRIBUTE = 2;
    public static final byte MISSING_WELLKNOWN_ATTRIBUTE = 3;
    public static final byte ATTRIBUTE_FLAGS_ERROR = 4;
    public static final byte ATTRIBUTE_LENGTH_ERROR = 5;
    public static final byte INVALID_ORIGIN_ATTRIBUTE = 6;
    public static final byte INVALID_NEXTHOP_ATTRIBUTE = 8;
    public static final byte OPTIONAL_ATTRIBUTE_ERROR = 9;
    public static final byte INVALID_NETWORK_FIELD = 10;
    public static final byte MALFORMED_ASPATH = 11;
}