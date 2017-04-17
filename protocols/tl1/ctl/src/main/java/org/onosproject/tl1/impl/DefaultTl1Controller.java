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
package org.onosproject.tl1.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.util.CharsetUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.tl1.Tl1Command;
import org.onosproject.tl1.Tl1Controller;
import org.onosproject.tl1.Tl1Device;
import org.onosproject.tl1.Tl1Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Implementation of TL1 controller.
 *
 * Handles the connection and input/output for all registered TL1 devices.
 * Turn on debug logging if you want to see all message I/O.
 *
 * Per device, we track commands using a simple ctag-keyed map. This assumes the client is sending out unique ctag's.
 */
@Component(immediate = true)
@Service
public class DefaultTl1Controller implements Tl1Controller {
    private final Logger log = LoggerFactory.getLogger(DefaultTl1Controller.class);

    // TL1 message delimiter (semi colon)
    private static final ByteBuf DELIMITER = Unpooled.copiedBuffer(new char[]{';'}, Charset.defaultCharset());
    private static final String COMPLD = "COMPLD";
    private static final String DENY = "DENY";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private ConcurrentMap<DeviceId, Tl1Device> deviceMap = new ConcurrentHashMap<>();
    // Key: channel, value: map with key ctag, value: future TL1 msg (ctags are assumed unique per device)
    private ConcurrentMap<Channel, ConcurrentMap<Integer, CompletableFuture<String>>> msgMap =
            new ConcurrentHashMap<>();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Set<Tl1Listener> tl1Listeners = new CopyOnWriteArraySet<>();
    private ExecutorService executor;

    @Activate
    public void activate() {
        executor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                Tools.groupedThreads("onos/tl1controller", "%d", log));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        executor.shutdown();
        deviceMap.clear();
        msgMap.clear();
        log.info("Stopped");
    }

    @Override
    /**
     * This implementation returns an empty string on failure.
     */
    public CompletableFuture<String> sendMsg(DeviceId deviceId, Tl1Command msg) {
        log.debug("Sending TL1 message to device {}: {}", deviceId, msg);

        Tl1Device device = deviceMap.get(deviceId);
        if (device == null || !device.isConnected() || !mastershipService.isLocalMaster(deviceId)) {
            return CompletableFuture.completedFuture(StringUtils.EMPTY);
        }

        // Create and store completable future, complete it in the channel handler when we receive a response
        CompletableFuture<String> future = new CompletableFuture<>();
        Channel channel = device.channel();
        if (!msgMap.containsKey(channel)) {
            return CompletableFuture.completedFuture(StringUtils.EMPTY);
        }
        msgMap.get(channel).put(msg.ctag(), future);

        // Write message to channel
        channel.writeAndFlush(Unpooled.copiedBuffer(msg.toString(), CharsetUtil.UTF_8));

        return future;
    }

    @Override
    public Optional<Tl1Device> getDevice(DeviceId deviceId) {
        return Optional.ofNullable(deviceMap.get(deviceId));
    }

    @Override
    public boolean addDevice(DeviceId deviceId, Tl1Device device) {
        log.debug("Adding TL1 device {} {}", deviceId);

        // Ignore if device already known
        if (deviceMap.containsKey(deviceId)) {
            log.error("Ignoring duplicate device {}", deviceId);
            return false;
        }

        deviceMap.put(deviceId, device);
        return true;
    }

    @Override
    public void connectDevice(DeviceId deviceId) {
        Tl1Device device = deviceMap.get(deviceId);
        if (device == null || device.isConnected()) {
            return;
        }

        Bootstrap b = new Bootstrap();
        b.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new DelimiterBasedFrameDecoder(8192, DELIMITER));
                        socketChannel.pipeline().addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
                        // TODO
                        //socketChannel.pipeline().addLast(new Tl1Decoder());
                        socketChannel.pipeline().addLast(new Tl1InboundHandler());
                    }
                })
                .remoteAddress(device.ip().toInetAddress(), device.port())
                .connect()
                .addListener((ChannelFuture channelFuture) -> {
                    if (channelFuture.isSuccess()) {
                        msgMap.put(channelFuture.channel(), new ConcurrentHashMap<>());
                        device.connect(channelFuture.channel());
                        tl1Listeners.forEach(l -> executor.execute(() -> l.deviceConnected(deviceId)));
                    }
                });
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        disconnectDevice(deviceId);
        deviceMap.remove(deviceId);
    }

    @Override
    public void addListener(Tl1Listener listener) {
        tl1Listeners.add(listener);
    }

    @Override
    public void removeListener(Tl1Listener listener) {
        tl1Listeners.remove(listener);
    }

    @Override
    public void disconnectDevice(DeviceId deviceId) {
        // Ignore if unknown device
        Tl1Device device = deviceMap.get(deviceId);
        if (device == null) {
            return;
        }

        Channel channel = device.channel();
        if (channel != null) {
            channel.close();
            msgMap.remove(channel);
        }

        device.disconnect();
        tl1Listeners.forEach(l -> l.deviceDisconnected(deviceId));
    }

    @Override
    public Set<DeviceId> getDeviceIds() {
        return deviceMap.keySet();
    }

    @Override
    public Collection<Tl1Device> getDevices() {
        return deviceMap.values();
    }

    /**
     * Crude filtering handler that will only complete our stored future upon receiving a TL1 response messages.
     */
    private class Tl1InboundHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String s) throws Exception {
            log.debug("Received TL1 message {}", s);

            // Search for "COMPLD" or "DENY" to identify a TL1 response,
            // then return the remainder of the string.
            String[] words = s.split("\\s");
            for (int i = 0; i < words.length; i++) {
                String w = words[i];
                if (w.startsWith(COMPLD) || w.startsWith(DENY)) {
                    // ctag is just in front of it
                    int ctag = Integer.parseInt(words[i - 1]);
                    // We return everything that follows to the caller (this will lose line breaks and such)
                    String result = Arrays.stream(words).skip(i + 1).collect(Collectors.joining());
                    // Set future when command is executed, good or bad
                    Map<Integer, CompletableFuture<String>> msg = msgMap.get(ctx.channel());
                    if (msg != null) {
                        CompletableFuture<String> f = msg.remove(ctag);
                        if (f != null) {
                            f.complete(result);
                        }
                    }

                    return;
                }
            }
        }
    }
}
