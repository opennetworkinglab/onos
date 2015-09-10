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
package org.onosproject.provider.pcep.tunnel.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.packet.IpAddress;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.driver.PcepAgent;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepVersion;

import com.google.common.collect.Sets;

public class PcepClientControllerAdapter implements PcepClientController {

    protected ConcurrentHashMap<PccId, PcepClient> connectedClients =
            new ConcurrentHashMap<PccId, PcepClient>();

    protected PcepClientAgent agent = new PcepClientAgent();
    protected Set<PcepClientListener> pcepClientListener = new HashSet<>();

    protected Set<PcepEventListener> pcepEventListener = Sets.newHashSet();

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
        //return connectedClients.get(pccIpAddress);
        PcepClientAdapter pc = new PcepClientAdapter();
        pc.init(PccId.pccId(IpAddress.valueOf(0xac000001)), PcepVersion.PCEP_1);
        return pc;
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
        case REPORT:
            for (PcepEventListener l : pcepEventListener) {
                l.handleMessage(pccId, msg);
            }
            break;
        case UPDATE:
            for (PcepEventListener l : pcepEventListener) {
                l.handleMessage(pccId, msg);
            }
            break;
        case INITIATE:
            for (PcepEventListener l : pcepEventListener) {
                l.handleMessage(pccId, msg);
            }
            break;
        case LABEL_UPDATE:
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
    }

}
