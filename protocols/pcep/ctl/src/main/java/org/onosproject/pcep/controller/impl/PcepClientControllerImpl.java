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
package org.onosproject.pcep.controller.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.driver.PcepAgent;
import org.onosproject.pcepio.protocol.PcepError;
import org.onosproject.pcepio.protocol.PcepErrorInfo;
import org.onosproject.pcepio.protocol.PcepErrorMsg;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepFactory;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import static org.onosproject.pcepio.types.PcepErrorDetailInfo.ERROR_TYPE_19;
import static org.onosproject.pcepio.types.PcepErrorDetailInfo.ERROR_VALUE_5;

/**
 * Implementation of PCEP client controller.
 */
@Component(immediate = true)
@Service
public class PcepClientControllerImpl implements PcepClientController {

    private static final Logger log = LoggerFactory.getLogger(PcepClientControllerImpl.class);

    protected ConcurrentHashMap<PccId, PcepClient> connectedClients =
            new ConcurrentHashMap<>();

    protected PcepClientAgent agent = new PcepClientAgent();
    protected Set<PcepClientListener> pcepClientListener = new HashSet<>();

    protected Set<PcepEventListener> pcepEventListener = Sets.newHashSet();

    private final Controller ctrl = new Controller();

    @Activate
    public void activate() {
        ctrl.start(agent);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        // Close all connected clients
        closeConnectedClients();
        ctrl.stop();
        log.info("Stopped");
    }

    @Override
    public Collection<PcepClient> getClients() {
        return connectedClients.values();
    }

    @Override
    public PcepClient getClient(PccId pccId) {
        return connectedClients.get(pccId);
    }

    @Override
    public void addListener(PcepClientListener listener) {
        if (!pcepClientListener.contains(listener)) {
            this.pcepClientListener.add(listener);
        }
    }

    @Override
    public void removeListener(PcepClientListener listener) {
        this.pcepClientListener.remove(listener);
    }

    @Override
    public void addEventListener(PcepEventListener listener) {
        pcepEventListener.add(listener);
    }

    @Override
    public void removeEventListener(PcepEventListener listener) {
        pcepEventListener.remove(listener);
    }

    @Override
    public void writeMessage(PccId pccId, PcepMessage msg) {
        this.getClient(pccId).sendMessage(msg);
    }

    @Override
    public void processClientMessage(PccId pccId, PcepMessage msg) {
        PcepClient pc = getClient(pccId);

        switch (msg.getType()) {
        case NONE:
            break;
        case OPEN:
            break;
        case KEEP_ALIVE:
            break;
        case PATH_COMPUTATION_REQUEST:
            break;
        case PATH_COMPUTATION_REPLY:
            break;
        case NOTIFICATION:
            break;
        case ERROR:
            break;
        case INITIATE:
            if (!pc.capability().pcInstantiationCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(), ERROR_TYPE_19,
                        ERROR_VALUE_5)));
            }
            break;
        case UPDATE:
            if (!pc.capability().statefulPceCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(), ERROR_TYPE_19,
                        ERROR_VALUE_5)));
            }
            break;
        case LABEL_UPDATE:
            if (!pc.capability().pceccCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(), ERROR_TYPE_19,
                        ERROR_VALUE_5)));
            }
            break;
        case CLOSE:
            log.info("Sending Close Message  to {" + pccId.toString() + "}");
            pc.sendMessage(Collections.singletonList(pc.factory().buildCloseMsg().build()));
            //now disconnect client
            pc.disconnectClient();
            break;
        case REPORT:
            //Only update the listener if respective capability is supported else send PCEP-ERR msg
            if (pc.capability().statefulPceCapability()) {
                for (PcepEventListener l : pcepEventListener) {
                    l.handleMessage(pccId, msg);
                }
            } else {
                // Send PCEP-ERROR message.
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(),
                        ERROR_TYPE_19, ERROR_VALUE_5)));
            }
            break;
        case LABEL_RANGE_RESERV:
            break;
        case MAX:
            break;
        case END:
            break;
        default:
            break;
        }
    }

    @Override
    public void closeConnectedClients() {
        PcepClient pc;
        for (PccId id : connectedClients.keySet()) {
            pc = getClient(id);
            pc.disconnectClient();
        }
    }

    /**
     * Returns pcep error message with specific error type and value.
     *
     * @param factory represents pcep factory
     * @param errorType pcep error type
     * @param errorValue pcep error value
     * @return pcep error message
     */
    public PcepErrorMsg getErrMsg(PcepFactory factory, byte errorType, byte errorValue) {
        LinkedList<PcepError> llPcepErr = new LinkedList<>();

        LinkedList<PcepErrorObject> llerrObj = new LinkedList<>();
        PcepErrorMsg errMsg;

        PcepErrorObject errObj = factory.buildPcepErrorObject().setErrorValue(errorValue).setErrorType(errorType)
                .build();

        llerrObj.add(errObj);
        PcepError pcepErr = factory.buildPcepError().setErrorObjList(llerrObj).build();

        llPcepErr.add(pcepErr);

        PcepErrorInfo errInfo = factory.buildPcepErrorInfo().setPcepErrorList(llPcepErr).build();

        errMsg = factory.buildPcepErrorMsg().setPcepErrorInfo(errInfo).build();
        return errMsg;
    }

    /**
     * Implementation of an Pcep Agent which is responsible for
     * keeping track of connected clients and the state in which
     * they are.
     */
    public class PcepClientAgent implements PcepAgent {

        private final Logger log = LoggerFactory.getLogger(PcepClientAgent.class);

        @Override
        public boolean addConnectedClient(PccId pccId, PcepClient pc) {

            if (connectedClients.get(pccId) != null) {
                log.error("Trying to add connectedClient but found a previous "
                        + "value for pcc ip: {}", pccId.toString());
                return false;
            } else {
                log.debug("Added Client {}", pccId.toString());
                connectedClients.put(pccId, pc);
                for (PcepClientListener l : pcepClientListener) {
                    l.clientConnected(pccId);
                }
                return true;
            }
        }

        @Override
        public boolean validActivation(PccId pccId) {
            if (connectedClients.get(pccId) == null) {
                log.error("Trying to activate client but is not in "
                        + "connected client: pccIp {}. Aborting ..", pccId.toString());
                return false;
            }

            return true;
        }

        @Override
        public void removeConnectedClient(PccId pccId) {

            connectedClients.remove(pccId);
            for (PcepClientListener l : pcepClientListener) {
                log.warn("removal for {}", pccId.toString());
                l.clientDisconnected(pccId);
            }
        }

        @Override
        public void processPcepMessage(PccId pccId, PcepMessage m) {
            processClientMessage(pccId, m);
        }
    }
}
