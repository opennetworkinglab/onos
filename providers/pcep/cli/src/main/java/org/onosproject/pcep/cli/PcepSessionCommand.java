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
package org.onosproject.pcep.cli;


import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepErrorDetail;
import org.onosproject.pcep.controller.PcepErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeMap;


@Command(scope = "onos", name = "pcep", description = "Pcep Session Info")
public class PcepSessionCommand extends AbstractShellCommand {
    private static final Logger log = LoggerFactory.getLogger(PcepSessionCommand.class);
    private static final String SESSION = "session";
    private static final String EXCEPTION = "exception";
    private static final String ERROR = "error";
    private PcepClientController pcepClientController;
    private byte sessionId;
    private Set<String> pcepSessionKeySet;
    private Set<String> pcepSessionIdKeySet;
    private Integer sessionIdValue = 0;
    private String sessionStatus;
    private List pcepSessionExceptions = new ArrayList();
    private Set<String> pcepSessionFailurekeySet;
    private PcepErrorDetail pcepErrorDetail;
    private PcepErrorType pcepErrorType;
    private Map<Integer, String> sessionEstablishmentFailureMap = new TreeMap<>();
    private Map<Integer, String> unknownObjectMap = new TreeMap<>();
    private Map<Integer, String> notSupportedObjectMap = new TreeMap<>();
    private Map<Integer, String> policyViolationMap = new TreeMap<>();
    private Map<Integer, String> mandatoryObjectMissingMap = new TreeMap<>();
    private Map<Integer, String> receptionOfInvalidObjectMap = new TreeMap<>();
    private Map<Integer, String> invalidOperationMap = new TreeMap<>();
    private Set<Integer> pcepErrorMsgKey;
    private Integer pcepErrorValue = 0;

    @Argument(index = 0, name = "name",
            description = "session" + "\n" + "exception" + "\n" + "error",
            required = true, multiValued = false)
    String name = null;
    @Argument(index = 1, name = "peer",
            description = "peerIp",
            required = false, multiValued = false)
    String peer = null;

    @Override
    protected void execute() {
        switch (name) {
            case SESSION:
                displayPcepSession();
                break;
            case EXCEPTION:
                displayPcepSessionFailureReason();
                break;
            case ERROR:
                displayPcepErrorMsgs();
                break;
            default:
                System.out.print("Unknown Command");
                break;
        }
    }

    private void displayPcepSession() {
        try {
            this.pcepClientController = get(PcepClientController.class);
            Map<String, String> pcepSessionMap = pcepClientController.getPcepSessionMap();
            Map<String, Byte> pcepSessionIdMap = pcepClientController.getPcepSessionIdMap();
            pcepSessionKeySet = pcepSessionMap.keySet();
            pcepSessionIdKeySet = pcepSessionIdMap.keySet();
            if (peer != null) {
                if (!pcepSessionKeySet.isEmpty()) {
                    if (pcepSessionKeySet.contains(peer)) {
                        for (String pcepSessionPeer : pcepSessionKeySet) {
                            if (pcepSessionPeer.equals(peer)) {
                                for (String pcepSessionId : pcepSessionIdKeySet) {
                                    if (pcepSessionId.equals(peer)) {
                                        sessionId = pcepSessionIdMap.get(pcepSessionId);
                                        sessionStatus = pcepSessionMap.get(pcepSessionPeer);
                                        if (sessionId < 0) {
                                            sessionIdValue = sessionId + 256;
                                        } else {
                                            sessionIdValue = (int) sessionId;
                                        }
                                    }
                                }
                  print("SessionIp = %s, Status = %s, sessionId = %s", pcepSessionPeer, sessionStatus, sessionIdValue);
                            }
                        }
                    } else {
                        System.out.print("Wrong Peer IP");
                    }
                }
            } else {
                if (!pcepSessionKeySet.isEmpty()) {
                    for (String pcepSessionPeer : pcepSessionKeySet) {
                        for (String pcepSessionId : pcepSessionIdKeySet) {
                            if (pcepSessionId.equals(pcepSessionPeer)) {
                                sessionId = pcepSessionIdMap.get(pcepSessionId);
                                sessionStatus = pcepSessionMap.get(pcepSessionPeer);
                                if (sessionId < 0) {
                                    sessionIdValue = sessionId + 256;
                                } else {
                                    sessionIdValue = (int) sessionId;
                                }
                            }
                        }
                print("SessionIp = %s, Status = %s, sessionId = %s", pcepSessionPeer, sessionStatus, sessionIdValue);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying PCEP session information: {}", e.getMessage());
        }
    }

    private void displayPcepSessionFailureReason() {
        try {
            this.pcepClientController = get(PcepClientController.class);
            Map<String, List<String>> pcepSessionFailureReasonMap = pcepClientController.getPcepExceptions();
            pcepSessionFailurekeySet = pcepSessionFailureReasonMap.keySet();
            if (!pcepSessionFailurekeySet.isEmpty()) {
                if (peer != null) {
                    if (pcepSessionFailurekeySet.contains(peer)) {
                        for (String pcepSessionPeerId : pcepSessionFailurekeySet) {
                            if (pcepSessionPeerId.equals(peer)) {
                                pcepSessionExceptions = pcepSessionFailureReasonMap.get(pcepSessionPeerId);
                                print("PeerId = %s, FailureReason = %s", pcepSessionPeerId, pcepSessionExceptions);
                            }
                        }
                    } else {
                        System.out.print("Wrong Peer IP");
                    }

                } else {
                    pcepSessionFailurekeySet = pcepSessionFailureReasonMap.keySet();
                    if (!pcepSessionFailurekeySet.isEmpty()) {
                        for (String pcepSessionPeerId : pcepSessionFailurekeySet) {
                            pcepSessionExceptions = pcepSessionFailureReasonMap.get(pcepSessionPeerId);
                            print("PeerId = %s, FailureReason = %s", pcepSessionPeerId, pcepSessionExceptions);
                        }
                    }
                }

            }


        } catch (Exception e) {
            log.debug("Error occurred while displaying PCEP session failure reasons: {}", e.getMessage());
        }

    }


    private void displayPcepErrorMsgs() {
        try {
            this.pcepClientController = get(PcepClientController.class);
            Map<Integer, Integer> pcepErrorMsgMap = pcepClientController.getPcepErrorMsg();
            pcepErrorMsgKey = pcepErrorMsgMap.keySet();
            if (!pcepErrorMsgKey.isEmpty()) {
                for (Integer errorType : pcepErrorMsgKey) {
                    pcepErrorValue = pcepErrorMsgMap.get(errorType);
                    pcepErrorType = PcepErrorType.values()[errorType];
                    switch (pcepErrorType) {
                        case SESSIONESTABLISHMENTFAILURE:
                            sessionEstablishmentFailureMap =  pcepErrorDetail.sessionEstablishmentFailure();
                            Set<Integer> sessionFailureKeySet = sessionEstablishmentFailureMap.keySet();
                            for (Integer sessionFailureKey : sessionFailureKeySet) {
                                if (sessionFailureKey.equals(pcepErrorValue)) {
                                    System.out.print(sessionEstablishmentFailureMap.get(sessionFailureKey));
                                }
                            }
                        case CAPABALITYNOTSUPPORTED:
                            System.out.print("Capability not supported");
                        case UNKNOWNOBJECT:
                            unknownObjectMap =  pcepErrorDetail.unknownObject();
                            Set<Integer> unknownObjectKeySet = unknownObjectMap.keySet();
                            for (Integer unknownObjectKey : unknownObjectKeySet) {
                                if (unknownObjectKey.equals(pcepErrorValue)) {
                                    System.out.print(unknownObjectMap.get(unknownObjectKey));
                                }
                            }
                        case NOTSUPPORTEDOBJECT:
                            notSupportedObjectMap =  pcepErrorDetail.notSupportedObject();
                            Set<Integer> notSupportedObjectKeySet = notSupportedObjectMap.keySet();
                            for (Integer notSupportedObjectKey : notSupportedObjectKeySet) {
                                if (notSupportedObjectKey.equals(pcepErrorValue)) {
                                    System.out.print(notSupportedObjectMap.get(notSupportedObjectKey));
                                }
                            }
                        case POLICYVIOLATION:
                            policyViolationMap =  pcepErrorDetail.policyViolation();
                            Set<Integer> policyViolationKeySet = policyViolationMap.keySet();
                            for (Integer policyViolationKey : policyViolationKeySet) {
                                if (policyViolationKey.equals(pcepErrorValue)) {
                                    System.out.print(policyViolationMap.get(policyViolationKey));
                                }
                            }
                        case MANDATORYOBJECTMISSING:
                            mandatoryObjectMissingMap =  pcepErrorDetail.mandatoryObjectMissing();
                            Set<Integer> mandatoryObjectMissingKeySet = mandatoryObjectMissingMap.keySet();
                            for (Integer mandatoryObjectMissingKey : mandatoryObjectMissingKeySet) {
                                if (mandatoryObjectMissingKey.equals(pcepErrorValue)) {
                                    System.out.print(mandatoryObjectMissingMap.get(mandatoryObjectMissingKey));
                                }
                            }
                        case SYNCHRONIZEDPATHCOMPUTATIONREQUESTMISSING:
                            System.out.print("Synchronized path computation request missing");
                        case UNKNOWNREQUESTREFERENCE:
                            System.out.print("Unknown request reference");
                        case ESTABLISHINGSECONDPCEPSESSION:
                            System.out.print("Attempt to establish a second PCEP session");
                        case RECEPTIONOFINVALIDOBJECT:
                            receptionOfInvalidObjectMap =  pcepErrorDetail.receptionOfInvalidObject();
                            Set<Integer> receptionOfInvalidObjectKeySet = receptionOfInvalidObjectMap.keySet();
                            for (Integer receptionOfInvalidObjectKey : receptionOfInvalidObjectKeySet) {
                                if (receptionOfInvalidObjectKey.equals(pcepErrorValue)) {
                                    System.out.print(receptionOfInvalidObjectMap.get(receptionOfInvalidObjectKey));
                                }
                            }
                        case INVALIDOPERATION:
                            invalidOperationMap =  pcepErrorDetail.invalidOperation();
                            Set<Integer> invalidOperationKeySet = invalidOperationMap.keySet();
                            for (Integer invalidOperationKey : invalidOperationKeySet) {
                                if (invalidOperationKey.equals(pcepErrorValue)) {
                                    System.out.print(invalidOperationMap.get(invalidOperationKey));
                                }
                            }
                        case VIRTUALNETWORKTLVMISSING:
                            System.out.print("VIRTUAL-NETWORK TLV missing");
                        default:
                            System.out.print("Unknown error message");
                    }
                }
            }
        }  catch (Exception e) {
            log.debug("Error occurred while displaying PCEP error messages received: {}", e.getMessage());
        }
    }
}
