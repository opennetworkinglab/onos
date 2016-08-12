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
package org.onosproject.openflow.controller;

import io.netty.buffer.ByteBuf;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;

import com.google.common.hash.PrimitiveSink;

import java.util.Arrays;

/**
 * Used to support for the third party privacy flow rule.
 * it implements OFMessage interface to use exist adapter API.
 */
public class ThirdPartyMessage implements OFMessage {

    private final byte[] payLoad; //privacy flow rule

    public ThirdPartyMessage(byte[] payLoad) {
        this.payLoad = payLoad;
    }

    public byte[] payLoad() {
        return payLoad;
    }

    @Override
    public void putTo(PrimitiveSink sink) {
     // Do nothing here for now.
    }

    @Override
    public OFVersion getVersion() {
     // Do nothing here for now.
        return null;
    }

    @Override
    public OFType getType() {
     // Do nothing here for now.
        return null;
    }

    @Override
    public long getXid() {
     // Do nothing here for now.
        return 0;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) {
     // Do nothing here for now.
    }

    @Override
    public boolean equalsIgnoreXid(Object obj) {
        return Arrays.equals(payLoad, ((ThirdPartyMessage) obj).payLoad());
    }

    @Override
    public int hashCodeIgnoreXid() {
        return payLoad.hashCode();
    }

    @Override
    public Builder createBuilder() {
     // Do nothing here for now.
        return null;
    }

}
