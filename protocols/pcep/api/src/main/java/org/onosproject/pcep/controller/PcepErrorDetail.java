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

import java.util.Map;
import java.util.TreeMap;

/**
 * PCEP error message details.
 */
public class PcepErrorDetail {

    private Map<Integer, String> sessionEstablishmentFailureMap = new TreeMap<>();
    private Map<Integer, String> unknownObjectMap = new TreeMap<>();
    private Map<Integer, String> notSupportedObjectMap = new TreeMap<>();
    private Map<Integer, String> policyViolationMap = new TreeMap<>();
    private Map<Integer, String> mandatoryObjectMissingMap = new TreeMap<>();
    private Map<Integer, String> receptionOfInvalidObjectMap = new TreeMap<>();
    private Map<Integer, String> invalidOperationMap = new TreeMap<>();


    public Map sessionEstablishmentFailure() {
        sessionEstablishmentFailureMap.put(1, "Reception of an invalid Open message or a non Open message.");
        sessionEstablishmentFailureMap.put(2, "no Open message received before the expiration of the OpenWait timer");
        sessionEstablishmentFailureMap.put(3, "unacceptable and non-negotiable session characteristics");
        sessionEstablishmentFailureMap.put(4, "unacceptable but negotiable session characteristics");
        sessionEstablishmentFailureMap.put(5, "reception of a second Open message with still " +
                "unacceptable session characteristics");
        sessionEstablishmentFailureMap.put(6, "reception of a PCErr message proposing unacceptable " +
                "session characteristics");
        sessionEstablishmentFailureMap.put(7, "No Keepalive or PCErr message received before the " +
                "expiration of the KeepWait timer");
        sessionEstablishmentFailureMap.put(8, "PCEP version not supported");
        return sessionEstablishmentFailureMap;
    }


    public Map unknownObject() {
        unknownObjectMap.put(1, "Unrecognized object class");
        unknownObjectMap.put(2, "Unrecognized object type");
        return unknownObjectMap;
    }

    public Map notSupportedObject() {
        notSupportedObjectMap.put(1, "Not Supported object class");
        notSupportedObjectMap.put(2, "Not Supported object type");
        return notSupportedObjectMap;
    }


    public Map policyViolation() {
        policyViolationMap.put(1, "C bit of the METRIC object set (request rejected)");
        policyViolationMap.put(2, "O bit of the RP object cleared (request rejected)");
        return policyViolationMap;
    }



    public Map mandatoryObjectMissing() {
        mandatoryObjectMissingMap.put(1, "RP object missing");
        mandatoryObjectMissingMap.put(2, "RRO missing for a re-optimization request (R bit of the RP object set)");
        mandatoryObjectMissingMap.put(2, "END-POINTS object missing");
        return mandatoryObjectMissingMap;

    }


    public Map receptionOfInvalidObject() {
        receptionOfInvalidObjectMap.put(1, "reception of an object with P flag not set although the P flag must be" +
                "set according to this specification.");
        return receptionOfInvalidObjectMap;
    }

    public Map invalidOperation() {
        invalidOperationMap.put(1, "Attempted LSP Update Request for a non-delegated LSP.  The PCEP-ERROR Object" +
                " is followed by the LSP Object that identifies the LSP.");
        invalidOperationMap.put(2, "Attempted LSP Update Request if the" +
                " stateful PCE capability was not" +
                " advertised.");
        invalidOperationMap.put(3, "Attempted LSP Update Request for an LSP" +
                "identified by an unknown PLSP-ID.");
        invalidOperationMap.put(4, "A PCE indicates to a PCC that it has" +
                " exceeded the resource limit allocated" +
                " for its state, and thus it cannot" +
                " accept and process its LSP State Report" +
                " message.");
        invalidOperationMap.put(5, "Attempted LSP State Report if active" +
                " stateful PCE capability was not" +
                " advertised.");
        return invalidOperationMap;
    }



}
