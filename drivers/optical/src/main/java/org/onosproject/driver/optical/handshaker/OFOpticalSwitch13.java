/*
 * Copyright 2016-present Open Networking Foundation
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

import org.projectfloodlight.openflow.protocol.OFExpPort;
import org.projectfloodlight.openflow.protocol.OFExpPortDescReply;
import org.projectfloodlight.openflow.protocol.OFExpPortDescRequest;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.OFStatsType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.onosproject.net.Device;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFObject;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;


/**
 * Open Flow Optical Switch handshaker - for Open Flow 13.
 */
public class OFOpticalSwitch13 extends AbstractOpenFlowSwitch implements OpenFlowOpticalSwitch {

    private final AtomicBoolean driverHandshakeComplete = new AtomicBoolean(false);
    private List<OFExpPort> expPortDes = new ArrayList<>();

    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public void startDriverHandshake() {
        log.info("Starting driver handshake for sw {}", getStringId());
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;

        log.debug("sendHandshakeOFExperimenterPortDescRequest for sw {}", getStringId());

        try {
            sendHandshakeOFExperimenterPortDescRequest();
        } catch (IOException e) {
            log.error("Failed to send handshaker message OFExperimenterPortDescRequestfor sw {}", e);
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

        log.debug("processDriverHandshakeMessage for sw {}", getStringId());

        switch (m.getType()) {
        case STATS_REPLY: // multipart message is reported as STAT
            processOFMultipartReply((OFStatsReply) m);
            break;
        default:
            log.warn("Received message {} during switch-driver " +
                    "subhandshake " + "from switch {} ... " +
                    "Ignoring message", m,
                    getStringId());
        }
    }

    private void processOFMultipartReply(OFStatsReply stats) {
        log.debug("Received message {} during switch-driver " +
                   "subhandshake " + "from switch {} ... " +
                   stats,
                   getStringId());

         if (stats.getStatsType() == OFStatsType.EXPERIMENTER) {
             try {
               OFExpPortDescReply expPortDescReply =  (OFExpPortDescReply) stats;
               expPortDes.addAll(expPortDescReply.getEntries());
               if (!expPortDescReply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
                   driverHandshakeComplete.set(true);
                   return;
               }
              } catch (ClassCastException e) {
                  log.error("Unexspected Experimenter Multipart message type {} ",
                          stats.getClass().getName());
            }
        }
    }


    @Override
    public boolean isDriverHandshakeComplete() {
        return driverHandshakeComplete.get();
    }

    private void sendHandshakeOFExperimenterPortDescRequest() throws
            IOException {

        OFExpPortDescRequest preq = factory()
                .buildExpPortDescRequest()
                .setXid(getNextTransactionId())
                .build();

        log.debug("Sending experimented port description " +
                "message " +
                "{}",
                preq.toString());

        this.sendHandshakeMessage(preq);
    }

    @Override
    public Device.Type deviceType() {
        String hwDesc = hardwareDescription();
        switch (hwDesc) {
            case "Optical-ROADM":
                return Device.Type.ROADM;
            case "Optical-OTN":
                return Device.Type.OTN;
            default:
                log.error("Unsupported hardwareDescription {}", hwDesc);
                return Device.Type.OTHER;
        }
    }

    /*
     * OduClt ports are reported as regular ETH ports.
     */
    @Override
    public List<OFPortDesc> getPorts() {
        return super.getPorts();
    }

    @Override
    public List<? extends OFObject> getPortsOf(PortDescPropertyType type) {
        return ImmutableList.copyOf(expPortDes);
    }

   @Override
    public Set<PortDescPropertyType> getPortTypes() {
        return ImmutableSet.of(PortDescPropertyType.OPTICAL_TRANSPORT);
    }

}
