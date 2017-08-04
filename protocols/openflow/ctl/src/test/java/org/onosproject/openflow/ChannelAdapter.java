/*
 * Copyright 2017-present Open Networking Foundation
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

import java.net.SocketAddress;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelProgressivePromise;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * Dummy Channel for testing.
 */
public class ChannelAdapter implements Channel {

    @Override
    public <T> Attribute<T> attr(AttributeKey<T> key) {

        return null;
    }

    @Override
    public int compareTo(Channel o) {
        return 0;
    }

    @Override
    public EventLoop eventLoop() {
        return null;
    }

    @Override
    public Channel parent() {
        return null;
    }

    @Override
    public ChannelConfig config() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public boolean isRegistered() {
        return false;
    }

    @Override
    public boolean isActive() {
        return false;
    }

    @Override
    public ChannelMetadata metadata() {
        return null;
    }

    @Override
    public SocketAddress localAddress() {
        return null;
    }

    @Override
    public SocketAddress remoteAddress() {
        return null;
    }

    @Override
    public ChannelFuture closeFuture() {
        return null;
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public Unsafe unsafe() {
        return null;
    }

    @Override
    public ChannelPipeline pipeline() {
        return null;
    }

    @Override
    public ByteBufAllocator alloc() {
        return null;
    }

    @Override
    public ChannelPromise newPromise() {
        return null;
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return null;
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return null;
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return null;
    }

    @Override
    public ChannelPromise voidPromise() {

        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {

        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {

        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress,
                                 SocketAddress localAddress) {

        return null;
    }

    @Override
    public ChannelFuture disconnect() {

        return null;
    }

    @Override
    public ChannelFuture close() {

        return null;
    }

    @Override
    public ChannelFuture deregister() {

        return null;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress,
                              ChannelPromise promise) {

        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress,
                                 ChannelPromise promise) {

        return null;
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress,
                                 SocketAddress localAddress,
                                 ChannelPromise promise) {

        return null;
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {

        return null;
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {

        return null;
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {

        return null;
    }

    @Override
    public Channel read() {

        return null;
    }

    @Override
    public ChannelFuture write(Object msg) {

        return null;
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {

        return null;
    }

    @Override
    public Channel flush() {

        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {

        return null;
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {

        return null;
    }

    @Override
    public <T> boolean hasAttr(AttributeKey<T> key) {

        return false;
    }

    @Override
    public ChannelId id() {

        return null;
    }

    @Override
    public long bytesBeforeUnwritable() {

        return 0;
    }

    @Override
    public long bytesBeforeWritable() {

        return 0;
    }

}
