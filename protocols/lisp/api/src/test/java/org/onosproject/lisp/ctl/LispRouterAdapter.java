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
package org.onosproject.lisp.ctl;

import com.google.common.collect.ImmutableList;
import io.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.net.Device;

import java.util.List;

/**
 * Test adapter for the LISP router interface.
 */
public class LispRouterAdapter implements LispRouter {

    private boolean subscribed;

    @Override
    public void sendMessage(LispMessage msg) {

    }

    @Override
    public void handleMessage(LispMessage msg) {

    }

    @Override
    public Device.Type deviceType() {
        return null;
    }

    @Override
    public String channelId() {
        return null;
    }

    @Override
    public void setChannel(Channel channel) {

    }

    @Override
    public String stringId() {
        return null;
    }

    @Override
    public IpAddress routerId() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public void setConnected(boolean connected) {

    }

    @Override
    public boolean isSubscribed() {
        return subscribed;
    }

    @Override
    public void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    @Override
    public List<LispEidRecord> getEidRecords() {
        return ImmutableList.of();
    }

    @Override
    public void setEidRecords(List<LispEidRecord> records) {

    }

    @Override
    public void setAgent(LispRouterAgent agent) {

    }

    @Override
    public boolean connectRouter() {
        return false;
    }

    @Override
    public void disconnectRouter() {

    }
}
