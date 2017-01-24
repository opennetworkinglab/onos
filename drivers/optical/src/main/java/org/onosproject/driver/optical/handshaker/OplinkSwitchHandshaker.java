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
package org.onosproject.driver.optical.handshaker;

import org.onosproject.net.behaviour.protection.ProtectionConfigBehaviour;
import org.projectfloodlight.openflow.protocol.OFCalientPortDescStatsEntry;
import org.projectfloodlight.openflow.protocol.OFCalientPortDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFCalientPortDescStatsRequest;
import org.projectfloodlight.openflow.protocol.OFCalientPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFCalientPortStatsRequest;
import org.projectfloodlight.openflow.protocol.OFCalientPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFCalientStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.Port;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static org.onosproject.net.optical.OpticalAnnotations.*;


/**
 * Oplink Open Flow Protection Optical Switch handshaker - for Open Flow 1.3.
 * In order to reduce the code changes in the short term, we reuse Calient message structure.
 */
public class OplinkSwitchHandshaker extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {

    private static final String PROTECTION_FINGERPRINT = "OplinkOPS";
    private static final int PROTECTION_VIRTUAL_PORT = 0;

    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private List<OFCalientPortDescStatsEntry> opticalPorts = new ArrayList<>();

    private enum SubType {
        PORT_DESC_STATS, // Port description stats openflow message
        FLOW_STATS,      // Flow stats openflow message
        PORT_STATS       // Port stats openflow message
    }

    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public void startDriverHandshake() {
        log.info("OPLK Switch: Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;

        log.debug("OPLK Switch: sendHandshakeOFExperimenterPortDescRequest for sw {}", getStringId());

        try {
            sendHandshakeOFExperimenterPortDescRequest();
        } catch (IOException e) {
            log.error("OPLK Switch: exception while sending experimenter port desc:", e);
        }
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }
        if (driverHandshakeComplete.get()) {
            throw new SwitchDriverSubHandshakeCompleted(m);
        }

        log.info("OPLK Switch: processDriverHandshakeMessage for sw {}", getStringId());

        switch (m.getType()) {
        case STATS_REPLY: // multipart message is reported as STAT
            processOFMultipartReply((OFStatsReply) m);
            driverHandshakeComplete.set(true);
            break;
        default:
            log.warn("OPLK Switch: Received message {} during switch-driver " +
                    "subhandshake from switch {} ... " +
                    "Ignoring message", m, getStringId());
        }
    }

    @Override
    public final void sendMsg(OFMessage m) {
        List<OFMessage> messages = new ArrayList<>();
        messages.add(m);
        if (m.getType() == OFType.STATS_REQUEST) {
            OFStatsRequest sr = (OFStatsRequest) m;
            log.debug("OPLK Switch: Rebuilding stats request type {}", sr.getStatsType());
            switch (sr.getStatsType()) {
                case PORT:
                    //Send experiment status request for Optical Fiber switch to device
                    //Note: We just re-use calient message for a short term.
                    OFCalientPortStatsRequest portRequest = this.factory().buildCalientPortStatsRequest()
                            .setXid(sr.getXid())
                            .setFlags(sr.getFlags())
                            .build();
                    messages.add(portRequest);
                    break;
                default:
                    break;
            }
        } else {
            log.debug("OPLK Switch: sends msg:{}, as is", m.getType());
        }
        super.sendMsg(messages);
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        return driverHandshakeComplete.get();
    }

    private void sendHandshakeOFExperimenterPortDescRequest() throws
            IOException {
        /*
         *Note:
         * Oplink protection switch and Calient switch are both optical fiber switch,
         * so Calient port description matches well for Oplink switch.
         * OFCalientPortDescStatsRequest is generated by loxi.
         * If change the OF message name, we need to change onos-loxi.
         * To reduce code change for a short term, we reuse calient message and message name.
         * These will be re-processed in the future.
         */
        OFCalientPortDescStatsRequest preq = factory()
                .buildCalientPortDescStatsRequest()
                .setXid(getNextTransactionId())
                .build();

        log.info("OPLK Switch: Sending experimented port description message {}", preq);

        this.sendHandshakeMessage(preq);
    }

    @Override
    public Device.Type deviceType() {
        return Device.Type.FIBER_SWITCH;
    }

    /*
     * OduClt ports are reported as regular ETH ports.
     */
    @Override
    public List<OFPortDesc> getPorts() {
        return ImmutableList.copyOf(
                ports.stream().flatMap(p -> p.getEntries().stream())
                .collect(Collectors.toList()));
    }

    @Override
    public List<? extends OFObject> getPortsOf(PortDescPropertyType type) {
        return ImmutableList.copyOf(opticalPorts);
    }

    @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL_TRANSPORT);
    }

    @Override
    public List<PortDescription> processExpPortStats(OFMessage msg) {
        OFCalientStatsReply statsReply = (OFCalientStatsReply) msg;
        //Sub type from openflowj is start from index 1
        SubType type = SubType.values()[(int) statsReply.getSubtype() - 1];
        switch (type) {
            case PORT_STATS:
                //Note: We just re-use calient message for a short term.
                OFCalientPortStatsReply portStats = (OFCalientPortStatsReply) msg;
                return buildPortDescriptions(portStats.getEntries());
            default:
                //Ignore other messages
                log.warn("OPLK Switch: Received message {} from switch {} ... " +
                    "Ignoring message", msg, getStringId());
                return null;
        }
    }

    private List<PortDescription> buildPortDescriptions(List<OFCalientPortStatsEntry> entries) {
        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(this.data().deviceId());
        HashMap<Long, OFCalientPortStatsEntry> statsMap = new HashMap<>(entries.size());
        entries.forEach(entry -> statsMap.put((long) entry.getPortNo().getPortNumber(), entry));
        final List<PortDescription> portDescs = new ArrayList<>();
        for (Port port : ports) {
            DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
            builder.putAll(port.annotations());

            //set fingerprint for the virtual port (port 0)
            if (port.number().toLong() == PROTECTION_VIRTUAL_PORT) {
                builder.set(ProtectionConfigBehaviour.FINGERPRINT, PROTECTION_FINGERPRINT);
            }

            OFCalientPortStatsEntry entry = statsMap.get(port.number().toLong());
            if (entry == null) {
                continue;
            }
            builder.set(CURRENT_POWER, entry.getInportPower());
            builder.set(OUTPUT_POWER, entry.getOutportPower());
            //Note: There are some mistakes about bitmask encoding and decoding in openflowj.
            //We just use this code for a short term, and will modify in the future.
            if (entry.getInOperStatus().isEmpty()) {
                builder.set(INPUT_PORT_STATUS, STATUS_IN_SERVICE);
            } else {
                builder.set(INPUT_PORT_STATUS, STATUS_OUT_SERVICE);
            }
            if (entry.getOutOperStatus().isEmpty()) {
                builder.set(OUTPUT_PORT_STATUS, STATUS_IN_SERVICE);
            } else {
                builder.set(OUTPUT_PORT_STATUS, STATUS_OUT_SERVICE);
            }
            portDescs.add(new DefaultPortDescription(port.number(), port.isEnabled(),
                    port.type(), port.portSpeed(), builder.build()));
        }
        return portDescs;
    }

    private void processOFMultipartReply(OFStatsReply stats) {
        log.debug("OPLK Switch: Received message {} during switch-driver " +
                   "subhandshake from switch {} ... ", stats, getStringId());
        //Process experimenter messages
        if (stats.getStatsType() == OFStatsType.EXPERIMENTER) {
            try {
                //Note: We just re-use calient message for a short term.
                OFCalientPortDescStatsReply descReply =  (OFCalientPortDescStatsReply) stats;
                opticalPorts.addAll(descReply.getPortDesc());
                driverHandshakeComplete.set(true);
            } catch (ClassCastException e) {
                log.error("OPLK Switch: Unexspected Experimenter Multipart message type {} ",
                        stats.getClass().getName());
            }
        }
    }
}
