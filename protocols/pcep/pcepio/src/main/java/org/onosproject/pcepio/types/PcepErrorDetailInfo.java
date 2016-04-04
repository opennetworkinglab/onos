/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.pcepio.types;

/**
 * Provide the PCEP Error Info Details.
 */
public final class PcepErrorDetailInfo {

    private PcepErrorDetailInfo() {
    }

    // Error Types
    /**
    Error-  Meaning                                           Reference:RFC 5440
    Type
    1     PCEP session establishment failure
        Error-value=1: reception of an invalid Open message or a non Open message.
        Error-value=2: no Open message received before the expiration of the OpenWait timer
        Error-value=3: unacceptable and non-negotiable session characteristics
        Error-value=4: unacceptable but negotiable session characteristics
        Error-value=5: reception of a second Open message with still unacceptable session characteristics
        Error-value=6: reception of a PCErr message proposing unacceptable session characteristics
        Error-value=7: No Keepalive or PCErr message received before the expiration of the KeepWait timer
        Error-value=8: PCEP version not supported
    2     Capability not supported
    3     Unknown Object
         Error-value=1: Unrecognized object class
         Error-value=2: Unrecognized object Type
    4     Not supported object
         Error-value=1: Not supported object class
         Error-value=2: Not supported object Type
    5     Policy violation
         Error-value=1: C bit of the METRIC object set (request rejected)
         Error-value=2: O bit of the RP object cleared (request rejected)
    6     Mandatory Object missing
         Error-value=1: RP object missing
         Error-value=2: RRO missing for a re-optimization request (R bit of the RP object set)
         Error-value=3: END-POINTS object missing
    7     Synchronized path computation request missing
    8     Unknown request reference
    9     Attempt to establish a second PCEP session
    10     Reception of an invalid object
         Error-value=1: reception of an object with P flag not set although the P flag must be
                        set according to this specification.

    Reference draft-ietf-pce-stateful-pce-11, section : 8.4
    19    Invalid Operation
         Error-value=1:  Attempted LSP Update Request for a non-
                                 delegated LSP.  The PCEP-ERROR Object
                                 is followed by the LSP Object that
                                 identifies the LSP.
         Error-value=2:  Attempted LSP Update Request if the
                                 stateful PCE capability was not
                                 advertised.
         Error-value=3:  Attempted LSP Update Request for an LSP
                                 identified by an unknown PLSP-ID.
         Error-value=4:  A PCE indicates to a PCC that it has
                                 exceeded the resource limit allocated
                                 for its state, and thus it cannot
                                 accept and process its LSP State Report
                                 message.
         Error-value=5:  Attempted LSP State Report if active
                                 stateful PCE capability was not
                                 advertised.
     */
    public static final byte ERROR_TYPE_1 = 1;
    public static final byte ERROR_TYPE_2 = 2;
    public static final byte ERROR_TYPE_3 = 3;
    public static final byte ERROR_TYPE_4 = 4;
    public static final byte ERROR_TYPE_5 = 5;
    public static final byte ERROR_TYPE_6 = 6;
    public static final byte ERROR_TYPE_7 = 7;
    public static final byte ERROR_TYPE_8 = 8;
    public static final byte ERROR_TYPE_9 = 9;
    public static final byte ERROR_TYPE_10 = 10;
    public static final byte ERROR_TYPE_19 = 19;

    // Error Values
    public static final byte ERROR_VALUE_1 = 1;
    public static final byte ERROR_VALUE_2 = 2;
    public static final byte ERROR_VALUE_3 = 3;
    public static final byte ERROR_VALUE_4 = 4;
    public static final byte ERROR_VALUE_5 = 5;
    public static final byte ERROR_VALUE_6 = 6;
    public static final byte ERROR_VALUE_7 = 7;
    public static final byte ERROR_VALUE_8 = 8;
    public static final byte ERROR_VALUE_9 = 9;
    public static final byte ERROR_VALUE_10 = 10;
}
