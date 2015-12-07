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
package org.onosproject.openflow.controller.driver;

import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.List;

/**
 * Mock of the Open Flow packet out message.
 */
public class MockOfPacketOut extends OfMessageAdapter implements OFPacketOut {

    public MockOfPacketOut() {
        super(OFType.PACKET_OUT);
    }

    @Override
    public OFBufferId getBufferId() {
        return null;
    }

    @Override
    public OFPort getInPort() {
        return null;
    }

    @Override
    public List<OFAction> getActions() {
        return null;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public OFPacketOut.Builder createBuilder() {
        return null;
    }
}
