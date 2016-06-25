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

package org.onosproject.pcepio.protocol;

/**
 * Enum to Provide the Different types of PCEP messages.
 */
public enum PcepType {

    NONE(0), OPEN(1), KEEP_ALIVE(2), PATH_COMPUTATION_REQUEST(3), PATH_COMPUTATION_REPLY(4),
    NOTIFICATION(5), ERROR(6), CLOSE(7), REPORT(10), UPDATE(11), INITIATE(12),
    LS_REPORT(224), LABEL_RANGE_RESERV(225), LABEL_UPDATE(226), MAX(227), END(228);

    int iValue;

    /**
     * Assign iValue with the value iVal as the types of PCEP message.
     *
     * @param iVal type of pcep message
     */
    PcepType(int iVal) {

        iValue = iVal;
    }

    /**
     * Returns iValue as type of PCEP message.
     *
     * @return iValue type of pcep message
     */
    public byte getType() {

        return (byte) iValue;
    }
}
