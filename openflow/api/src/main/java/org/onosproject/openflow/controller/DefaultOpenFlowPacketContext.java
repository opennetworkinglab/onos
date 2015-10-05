/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.openflow.controller;

import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ethernet;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferUnderflowException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.*;


/**
 * Default implementation of an OpenFlowPacketContext.
 */
public final class DefaultOpenFlowPacketContext implements OpenFlowPacketContext {

    private final AtomicBoolean free = new AtomicBoolean(true);
    private final AtomicBoolean isBuilt = new AtomicBoolean(false);
    private final OpenFlowSwitch sw;
    private final OFPacketIn pktin;
    private OFPacketOut pktout = null;

    private final boolean isBuffered;

    private DefaultOpenFlowPacketContext(OpenFlowSwitch s, OFPacketIn pkt) {
        this.sw = s;
        this.pktin = pkt;
        this.isBuffered = pktin.getBufferId() != OFBufferId.NO_BUFFER;
    }

    @Override
    public void send() {
        checkPermission(PACKET_WRITE);

        if (block() && isBuilt.get()) {
            sw.sendMsg(pktout);
        }
    }

    @Override
    public void build(OFPort outPort) {
        if (isBuilt.getAndSet(true)) {
            return;
        }
        OFPacketOut.Builder builder = sw.factory().buildPacketOut();
        OFAction act = buildOutput(outPort.getPortNumber());
        pktout = builder.setXid(pktin.getXid())
                .setInPort(pktinInPort())
                .setBufferId(OFBufferId.NO_BUFFER)
                .setData(pktin.getData())
//                .setBufferId(pktin.getBufferId())
                .setActions(Collections.singletonList(act))
                .build();
    }

    @Override
    public void build(Ethernet ethFrame, OFPort outPort) {
        if (isBuilt.getAndSet(true)) {
            return;
        }
        OFPacketOut.Builder builder = sw.factory().buildPacketOut();
        OFAction act = buildOutput(outPort.getPortNumber());
        pktout = builder.setXid(pktin.getXid())
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInPort(pktinInPort())
                .setActions(Collections.singletonList(act))
                .setData(ethFrame.serialize())
                .build();
    }

    @Override
    public Ethernet parsed() {
        checkPermission(PACKET_READ);

        try {
            return Ethernet.deserializer().deserialize(pktin.getData(), 0, pktin.getData().length);
        } catch (BufferUnderflowException | NullPointerException |
                DeserializationException e) {
            Logger log = LoggerFactory.getLogger(getClass());
            log.error("packet deserialization problem : {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Dpid dpid() {
        checkPermission(PACKET_READ);

        return new Dpid(sw.getId());
    }

    /**
     * Creates an OpenFlow packet context based on a packet-in.
     *
     * @param s OpenFlow switch
     * @param pkt OpenFlow packet-in
     * @return the OpenFlow packet context
     */
    public static OpenFlowPacketContext packetContextFromPacketIn(OpenFlowSwitch s,
                                                                  OFPacketIn pkt) {
        return new DefaultOpenFlowPacketContext(s, pkt);
    }

    @Override
    public Integer inPort() {
        checkPermission(PACKET_READ);

        return pktinInPort().getPortNumber();
    }

    private OFPort pktinInPort() {
        if (pktin.getVersion() == OFVersion.OF_10) {
            return pktin.getInPort();
        }
        return pktin.getMatch().get(MatchField.IN_PORT);
    }

    @Override
    public byte[] unparsed() {
        checkPermission(PACKET_READ);

        return pktin.getData().clone();

    }

    private OFActionOutput buildOutput(Integer port) {
        OFActionOutput act = sw.factory().actions()
                .buildOutput()
                .setPort(OFPort.of(port))
                .build();
        return act;
    }

    @Override
    public boolean block() {
        checkPermission(PACKET_WRITE);

        return free.getAndSet(false);
    }

    @Override
    public boolean isHandled() {
        checkPermission(PACKET_READ);

        return !free.get();
    }

    @Override
    public boolean isBuffered() {
        checkPermission(PACKET_READ);

        return isBuffered;
    }

}
