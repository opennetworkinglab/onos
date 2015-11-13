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
package org.onosproject.openflow;

import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;

/**
 * Mock of the Open Flow packet in message.
 */
public class MockOfPacketIn extends OfMessageAdapter implements OFPacketIn {
    public MockOfPacketIn() {
        super(OFType.PACKET_IN);
    }

    @Override
    public OFBufferId getBufferId() {
        return null;
    }

    @Override
    public int getTotalLen() {
        return 0;
    }

    @Override
    public OFPacketInReason getReason() {
        return null;
    }

    @Override
    public TableId getTableId() {
        return null;
    }

    @Override
    public Match getMatch() {
        return null;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    @Override
    public OFPort getInPort() {
        return null;
    }

    @Override
    public OFPort getInPhyPort() {
        return null;
    }

    @Override
    public U64 getCookie() {
        return null;
    }

    @Override
    public OFPacketIn.Builder createBuilder() {
        return null;
    }
}
