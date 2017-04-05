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
package org.onosproject.pcep.controller;

/**
 * PCEP error message type information.
 */
public enum  PcepErrorType {
    SESSIONESTABLISHMENTFAILURE(1),
    CAPABALITYNOTSUPPORTED(2),
    UNKNOWNOBJECT(3),
    NOTSUPPORTEDOBJECT(4),
    POLICYVIOLATION(5),
    MANDATORYOBJECTMISSING(6),
    SYNCHRONIZEDPATHCOMPUTATIONREQUESTMISSING(7),
    UNKNOWNREQUESTREFERENCE(8),
    ESTABLISHINGSECONDPCEPSESSION(9),
    RECEPTIONOFINVALIDOBJECT(10),
    INVALIDOPERATION(19),
    VIRTUALNETWORKTLVMISSING(255);

    int value;

    /**
     * Creates an instance of Pcep Error Type.
     *
     * @param value represents Error type
     */
    PcepErrorType(int value) {
        this.value = value;
    }

    /**
     * Gets the value representing Pcep Error Type.
     *
     * @return value represents Error Type
     */
    public  int value() {
        return value;
    }
}
