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
package org.onosproject.provider.pcep.topology.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.packet.IpAddress;
import org.onosproject.incubator.net.tunnel.DefaultLabelStack;
import org.onosproject.incubator.net.tunnel.LabelStack;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.net.Path;
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.PcepNodeListener;
import org.onosproject.pcep.controller.driver.PcepAgent;
import org.onosproject.pcepio.protocol.PcepError;
import org.onosproject.pcepio.protocol.PcepErrorInfo;
import org.onosproject.pcepio.protocol.PcepErrorMsg;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepFactory;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepValueType;

import com.google.common.collect.Sets;

import static org.onosproject.pcepio.types.PcepErrorDetailInfo.ERROR_TYPE_19;
import static org.onosproject.pcepio.types.PcepErrorDetailInfo.ERROR_VALUE_5;

/**
 * Representation of PCEP client controller adapter.
 */
public class PcepClientControllerAdapter implements PcepClientController {

    protected ConcurrentHashMap<PccId, PcepClient> connectedClients =
            new ConcurrentHashMap<PccId, PcepClient>();

    protected PcepClientAgent agent = new PcepClientAgent();
    protected Set<PcepClientListener> pcepClientListener = new HashSet<>();

    protected Set<PcepEventListener> pcepEventListener = Sets.newHashSet();
    public Set<PcepNodeListener> pcepNodeListener = Sets.newHashSet();

    @Activate
    public void activate() {
    }

    @Deactivate
    public void deactivate() {
    }

    @Override
    public Collection<PcepClient> getClients() {
        return connectedClients.values();
    }

    @Override
    public PcepClient getClient(PccId pccId) {
        if (null != connectedClients.get(pccId)) {
            return connectedClients.get(pccId);
        }
        PcepClientAdapter pc = new PcepClientAdapter();
        if (pccId.ipAddress().equals(IpAddress.valueOf(0xC010103))
            || pccId.ipAddress().equals(IpAddress.valueOf(0xB6024E22))) {
            pc.setCapability(new ClientCapability(true, false, false, false, false));
        } else {
            pc.setCapability(new ClientCapability(true, true, true, false, false));
        }
        pc.init(PccId.pccId(pccId.ipAddress()), PcepVersion.PCEP_1);
        connectedClients.put(pccId, pc);
        return pc;
    }

    @Override
    public void addListener(PcepClientListener listener) {
        if (!pcepClientListener.contains(listener)) {
            this.pcepClientListener.add(listener);
        }
    }

    @Override
    public void addNodeListener(PcepNodeListener listener) {
        pcepNodeListener.add(listener);
    }

    @Override
    public void removeNodeListener(PcepNodeListener listener) {
        pcepNodeListener.remove(listener);
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
            //log.debug("Sending Keep Alive Message  to {" + pccIpAddress.toString() + "}");
            pc.sendMessage(Collections.singletonList(pc.factory().buildKeepaliveMsg().build()));
            break;
        case PATH_COMPUTATION_REQUEST:
            break;
        case PATH_COMPUTATION_REPLY:
            break;
        case NOTIFICATION:
            break;
        case ERROR:
            break;
        case CLOSE:
            //log.debug("Sending Close Message  to { }", pccIpAddress.toString());
            pc.sendMessage(Collections.singletonList(pc.factory().buildCloseMsg().build()));
            break;
        case INITIATE:
            if (!pc.capability().pcInstantiationCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(),
                        ERROR_TYPE_19, ERROR_VALUE_5)));
            }
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
        case UPDATE:
            if (!pc.capability().statefulPceCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(),
                       ERROR_TYPE_19, ERROR_VALUE_5)));
            }
            break;
        case LABEL_UPDATE:
            if (!pc.capability().pceccCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(),
                        ERROR_TYPE_19, ERROR_VALUE_5)));
            }
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

    private PcepErrorMsg getErrMsg(PcepFactory factory, byte errorType, byte errorValue) {
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

        @Override
        public boolean addConnectedClient(PccId pccId, PcepClient pc) {

            if (connectedClients.get(pccId) != null) {
                return false;
            } else {
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
                //log.error("Trying to activate client but is not in "
                //        + "connected switches: pccIp {}. Aborting ..", pccIpAddress.toString());
                return false;
            }

            return true;
        }

        @Override
        public void removeConnectedClient(PccId pccId) {
            connectedClients.remove(pccId);
            for (PcepClientListener l : pcepClientListener) {
                //log.warn("removal for {}", pccIpAddress.toString());
                l.clientDisconnected(pccId);
            }
        }

        @Override
        public void processPcepMessage(PccId pccId, PcepMessage m) {
            processClientMessage(pccId, m);
        }

        @Override
        public void addNode(PcepClient pc) {
            for (PcepNodeListener l : pcepNodeListener) {
                l.addDevicePcepConfig(pc);
            }
        }

        @Override
        public void deleteNode(PccId pccId) {
            for (PcepNodeListener l : pcepNodeListener) {
                l.deleteDevicePcepConfig(pccId);
            }
        }

        @Override
        public boolean analyzeSyncMsgList(PccId pccId) {
            // TODO Auto-generated method stub
            return false;
        }
    }

    @Override
    public LabelStack computeLabelStack(Path path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public LinkedList<PcepValueType> createPcepLabelStack(DefaultLabelStack labelStack, Path path) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean allocateLocalLabel(Tunnel tunnel) {
        // TODO Auto-generated method stub
        return false;
    }
}
