/*
 * Copyright 2015-present Open Networking Laboratory
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

import java.util.List;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFAuxId;

/**
 * Mock of the Open FLow features reply message.
 */
public class MockOfFeaturesReply extends OfMessageAdapter implements OFFeaturesReply {
    public MockOfFeaturesReply() {
        super(OFType.FEATURES_REPLY);
    }

    @Override
    public DatapathId getDatapathId() {
        return null;
    }

    @Override
    public long getNBuffers() {
        return 0;
    }

    @Override
    public short getNTables() {
        return 0;
    }

    @Override
    public Set<OFCapabilities> getCapabilities() {
        return null;
    }

    @Override
    public long getReserved() {
        return 0;
    }

    @Override
    public List<OFPortDesc> getPorts() {
        return null;
    }

    @Override
    public Set<OFActionType> getActions() {
        return null;
    }

    @Override
    public OFAuxId getAuxiliaryId() {
        return null;
    }

    @Override
    public OFFeaturesReply.Builder createBuilder() {
        return null;
    }
}
