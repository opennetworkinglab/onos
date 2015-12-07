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


import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.OFGroup;

import java.util.List;
import java.util.Set;

/**
 * Mock of the Open Flow flow mod message.
 */
public class MockOfFlowMod extends OfMessageAdapter implements OFFlowMod {

    public MockOfFlowMod() {
        super(OFType.FLOW_MOD);
    }

    @Override
    public U64 getCookie() {
        return null;
    }

    @Override
    public U64 getCookieMask() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public TableId getTableId() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public OFFlowModCommand getCommand() {
        return null;
    }

    @Override
    public int getIdleTimeout() {
        return 0;
    }

    @Override
    public int getHardTimeout() {
        return 0;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public OFBufferId getBufferId() {
        return null;
    }

    @Override
    public OFPort getOutPort() {
        return null;
    }

    @Override
    public OFGroup getOutGroup() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public Set<OFFlowModFlags> getFlags() {
        return null;
    }

    @Override
    public Match getMatch() {
        return null;
    }

    @Override
    public List<OFInstruction> getInstructions() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public List<OFAction> getActions() throws UnsupportedOperationException {
        return null;
    }

    @Override
    public OFFlowMod.Builder createBuilder() {
        return null;
    }
}
