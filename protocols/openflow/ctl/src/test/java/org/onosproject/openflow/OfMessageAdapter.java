/*
 * Copyright 2015-present Open Networking Foundation
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

import io.netty.buffer.ByteBuf;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;

import com.google.common.hash.PrimitiveSink;

/**
 * Adapter for testing against an OpenFlow message.
 */
public class OfMessageAdapter implements OFMessage {
    OFType type;

    private OfMessageAdapter() {}

    public OfMessageAdapter(OFType type) {
        this.type = type;
    }

    @Override
    public OFType getType() {
        return type;
    }

    @Override
    public OFVersion getVersion() {
        return null;
    }

    @Override
    public long getXid() {
        return 0;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) { }

    @Override
    public boolean equalsIgnoreXid(Object obj) {
     // Do nothing here for now
        return true;
    }

    @Override
    public int hashCodeIgnoreXid() {
     // Do nothing here for now
        return 0;
    }

    @Override
    public Builder createBuilder() {
        return null;
    }

    @Override
    public void putTo(PrimitiveSink sink) { }
}
