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
package org.onosproject.openflow.controller.impl;

import org.onosproject.openflow.OfMessageAdapter;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;

/**
 * Mock of the Open Flow packet removed message.
 */
public class MockOfFlowRemoved extends OfMessageAdapter implements OFFlowRemoved {

    public MockOfFlowRemoved() {
        super(OFType.FLOW_REMOVED);
    }

    @Override
    public U64 getCookie() {
        return null;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public short getReason() {
        return 0;
    }

    @Override
    public TableId getTableId() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public long getDurationSec() {
        return 0;
    }

    @Override
    public long getDurationNsec() {
        return 0;
    }

    @Override
    public int getIdleTimeout() {
        return 0;
    }

    @Override
    public int getHardTimeout() throws UnsupportedOperationException {
        return 0;
    }

    @Override
    public U64 getPacketCount() {
        return null;
    }

    @Override
    public U64 getByteCount() {
        return null;
    }

    @Override
    public Match getMatch() {
        return null;
    }

    @Override
    public OFFlowRemoved.Builder createBuilder() {
        return null;
    }
}
