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

import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;

import com.google.common.hash.PrimitiveSink;

/**
 * Created by ray on 11/4/15.
 */
public class OFDescStatsReplyAdapter implements OFDescStatsReply {
    @Override
    public OFVersion getVersion() {
        return null;
    }

    @Override
    public OFType getType() {
        return null;
    }

    @Override
    public long getXid() {
        return 0;
    }

    @Override
    public OFStatsType getStatsType() {
        return null;
    }

    @Override
    public Set<OFStatsReplyFlags> getFlags() {
        return null;
    }

    @Override
    public String getMfrDesc() {
        return null;
    }

    @Override
    public String getHwDesc() {
        return null;
    }

    @Override
    public String getSwDesc() {
        return null;
    }

    @Override
    public String getSerialNum() {
        return null;
    }

    @Override
    public String getDpDesc() {
        return null;
    }

    @Override
    public void writeTo(ChannelBuffer channelBuffer) {

    }

    @Override
    public Builder createBuilder() {
        return null;
    }

    @Override
    public void putTo(PrimitiveSink sink) {

    }
}
