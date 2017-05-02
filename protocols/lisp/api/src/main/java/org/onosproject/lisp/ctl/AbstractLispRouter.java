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
package org.onosproject.lisp.ctl;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import io.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispMessage;
import org.onosproject.net.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

/**
 * An abstract representation of a LISP router.
 * This class can be extended by others to serve as a base for their vendor
 * specific representation of a router.
 */
public abstract class AbstractLispRouter implements LispRouter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DROP_MESSAGE_WARN =
                    "Drop message {} destined to router {} as channel is closed.";

    private Channel channel;
    private String channelId;

    private boolean connected;
    private boolean subscribed;
    private LispRouterId routerId;
    private LispRouterAgent agent;
    private List<LispEidRecord> records;

    /**
     * A default constructor.
     *
     * @param routerId router identifier
     */
    AbstractLispRouter(LispRouterId routerId) {
        this.routerId = routerId;
    }

    @Override
    public final String channelId() {
        return channelId;
    }

    @Override
    public final IpAddress routerId() {
        return routerId.id();
    }

    @Override
    public final String stringId() {
        return routerId.toString();
    }

    @Override
    public final void setChannel(Channel channel) {
        this.channel = channel;
        final SocketAddress address = channel.remoteAddress();
        if (address instanceof InetSocketAddress) {
            channelId = genChannelId((InetSocketAddress) address);
        }
    }

    @Override
    public final void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public final boolean isConnected() {
        return connected;
    }

    @Override
    public final boolean isSubscribed() {
        return subscribed;
    }

    @Override
    public final void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }

    @Override
    public final void setAgent(LispRouterAgent agent) {
        // we never assign the agent more than one time
        if (this.agent == null) {
            this.agent = agent;
        }
    }

    @Override
    public final Device.Type deviceType() {
        return Device.Type.ROUTER;
    }

    @Override
    public void sendMessage(LispMessage message) {
        if (channel.isOpen()) {
            // TODO: need to consider to use writeAndFlush if possible
            channel.write(message);
            agent.processDownstreamMessage(routerId, message);
        } else {
            log.warn(DROP_MESSAGE_WARN, message, routerId);
        }
    }

    @Override
    public void handleMessage(LispMessage message) {
        this.agent.processUpstreamMessage(routerId, message);
    }

    @Override
    public final boolean connectRouter() {
        setConnected(true);
        return this.agent.addConnectedRouter(routerId, this);
    }

    @Override
    public final void disconnectRouter() {
        setConnected(false);
        channel.close();
    }

    @Override
    public List<LispEidRecord> getEidRecords() {
        return records;
    }

    @Override
    public void setEidRecords(List<LispEidRecord> records) {
        this.records = ImmutableList.copyOf(records);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName());

        String address = (channel != null) ? channel.remoteAddress().toString() : "?";
        String routerId = (stringId() != null) ? stringId() : "?";

        sb.append(" [");
        sb.append(address);
        sb.append(" routerId[");
        sb.append(routerId);
        sb.append("]]");

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractLispRouter that = (AbstractLispRouter) o;
        return Objects.equal(channel, that.channel) &&
                Objects.equal(channelId, that.channelId) &&
                Objects.equal(connected, that.connected) &&
                Objects.equal(subscribed, that.subscribed) &&
                Objects.equal(routerId, that.routerId) &&
                Objects.equal(agent, that.agent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(channel, channelId, connected,
                                                    subscribed, routerId, agent);
    }

    /**
     * Generates a string format of channel ID from the given InetSocketAddress.
     *
     * @param inetAddress InetAddress object
     * @return string format of channel ID
     */
    private String genChannelId(InetSocketAddress inetAddress) {
        StringBuilder sb = new StringBuilder();
        final IpAddress ipAddress = IpAddress.valueOf(inetAddress.getAddress());
        if (ipAddress.isIp4()) {
            sb.append(ipAddress.toString());
            sb.append(":");
            sb.append(inetAddress.getPort());
        } else {
            sb.append("[");
            sb.append(ipAddress.toString());
            sb.append("]:");
            sb.append(inetAddress.getPort());
        }
        return sb.toString();
    }
}
