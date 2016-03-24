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
package org.onosproject.drivers.corsa;

import org.onosproject.net.meter.MeterId;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeAlreadyStarted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeCompleted;
import org.onosproject.openflow.controller.driver.SwitchDriverSubHandshakeNotStarted;
import org.projectfloodlight.openflow.protocol.OFBarrierRequest;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFGroupType;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterMod;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.TableId;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Corsa switch handshaker.
 */
public class CorsaSwitchHandshaker extends AbstractOpenFlowSwitch {

    private AtomicBoolean handshakeComplete = new AtomicBoolean(false);

    private int barrierXid;


    @Override
    public Boolean supportNxRole() {
        return false;
    }

    @Override
    public void startDriverHandshake() {
        if (startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        startDriverHandshakeCalled = true;
        OFFlowMod fm = factory().buildFlowDelete()
                .setTableId(TableId.ALL)
                .setOutGroup(OFGroup.ANY)
                .build();

        sendMsg(Collections.singletonList(fm));

        OFGroupMod gm = factory().buildGroupDelete()
                .setGroup(OFGroup.ALL)
                .setGroupType(OFGroupType.ALL)
                .build();

        sendMsg(Collections.singletonList(gm));

        OFMeterMod mm = factory().buildMeterMod()
                .setMeterId(MeterId.ALL.id())
                .build();

        sendMsg(Collections.singletonList(mm));

        barrierXid = getNextTransactionId();
        OFBarrierRequest barrier = factory().buildBarrierRequest()
                .setXid(barrierXid).build();


        sendHandshakeMessage(barrier);

    }

    @Override
    public boolean isDriverHandshakeComplete() {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeAlreadyStarted();
        }
        return handshakeComplete.get();
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        if (!startDriverHandshakeCalled) {
            throw new SwitchDriverSubHandshakeNotStarted();
        }
        if (handshakeComplete.get()) {
            throw new SwitchDriverSubHandshakeCompleted(m);
        }
        if (m.getType() == OFType.BARRIER_REPLY &&
                m.getXid() == barrierXid) {
            handshakeComplete.set(true);
        }
    }

}
