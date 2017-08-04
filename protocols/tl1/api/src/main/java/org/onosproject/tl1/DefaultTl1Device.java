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
package org.onosproject.tl1;

import io.netty.channel.Channel;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a TL1 device.
 */
public class DefaultTl1Device implements Tl1Device {
    private final Logger log = LoggerFactory.getLogger(DefaultTl1Device.class);
    private static final String TL1 = "tl1";

    private IpAddress ip;
    private int port;
    private String username;
    private String password;
    private String tid;
    private Channel channel;

    @Override
    public void connect(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void disconnect() {
        this.channel = null;
    }

    @Override
    public boolean isConnected() {
        return channel != null;
    }

    @Override
    public IpAddress ip() {
        return ip;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public Channel channel() {
        return channel;
    }

    @Override
    public String tid() {
        return tid;
    }

    public DefaultTl1Device(IpAddress ip, int port, String username, String password) {
        this.ip = checkNotNull(ip);
        checkArgument((TpPort.MIN_PORT <= port) && (port <= TpPort.MAX_PORT));
        this.port = port;
        this.username = checkNotNull(username);
        this.password = checkNotNull(password);
        this.tid = null;
        channel = null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, port, username, password);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultTl1Device) {
            DefaultTl1Device that = (DefaultTl1Device) obj;
            return Objects.equals(ip, that.ip) &&
                    Objects.equals(port, that.port) &&
                    Objects.equals(username, that.username) &&
                    Objects.equals(password, that.password) &&
                    Objects.equals(tid, that.tid) &&
                    Objects.equals(channel, that.channel);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("ip", ip)
                .add("port", port)
                .add("username", username)
                .add("password", password)
                .add("tid", tid == null ? "N/A" : tid)
                .toString();
    }
}
