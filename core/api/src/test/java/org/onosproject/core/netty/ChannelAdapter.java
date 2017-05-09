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
package org.onosproject.core.netty;

import java.net.SocketAddress;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * Adapter for testing against a netty channel.
 *
 * @deprecated in 1.11.0
 */
@Deprecated
public class ChannelAdapter implements Channel {
    @Override
    public Integer getId() {
        return null;
    }

    @Override
    public ChannelFactory getFactory() {
        return null;
    }

    @Override
    public Channel getParent() {
        return null;
    }

    @Override
    public ChannelConfig getConfig() {
        return null;
    }

    @Override
    public ChannelPipeline getPipeline() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public ChannelFuture write(Object o) {
        return null;
    }

    @Override
    public ChannelFuture write(Object o, SocketAddress socketAddress) {
        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress socketAddress) {
        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress socketAddress) {
        return null;
    }

    @Override
    public ChannelFuture disconnect() {
        return null;
    }

    @Override
    public ChannelFuture unbind() {
        return null;
    }

    @Override
    public ChannelFuture close() {
        return null;
    }

    @Override
    public ChannelFuture getCloseFuture() {
        return null;
    }

    @Override
    public int getInterestOps() {
        return 0;
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public ChannelFuture setInterestOps(int i) {
        return null;
    }

    @Override
    public ChannelFuture setReadable(boolean b) {
        return null;
    }

    @Override
    public boolean getUserDefinedWritability(int i) {
        return false;
    }

    @Override
    public void setUserDefinedWritability(int i, boolean b) {

    }

    @Override
    public Object getAttachment() {
        return null;
    }

    @Override
    public void setAttachment(Object o) {

    }

    @Override
    public int compareTo(Channel o) {
        return 0;
    }
}
