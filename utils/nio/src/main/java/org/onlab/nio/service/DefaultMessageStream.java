/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.nio.service;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.onlab.nio.IOLoop;
import org.onlab.nio.MessageStream;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onosproject.store.cluster.messaging.Endpoint;

import com.google.common.base.Charsets;

/**
 * Default bi-directional message stream for transferring messages to &amp; from the
 * network via two byte buffers.
 */
public class DefaultMessageStream extends MessageStream<DefaultMessage> {

    private final CompletableFuture<Void> connectFuture = new CompletableFuture<>();

    public DefaultMessageStream(
            IOLoop<DefaultMessage, ?> loop,
            ByteChannel byteChannel,
            int bufferSize,
            int maxIdleMillis) {
        super(loop, byteChannel, bufferSize, maxIdleMillis);
    }

    public CompletableFuture<DefaultMessageStream> connectedFuture() {
        return connectFuture.thenApply(v -> this);
    }

    private final AtomicInteger messageLength = new AtomicInteger(-1);

    @Override
    protected DefaultMessage read(ByteBuffer buffer) {
        if (messageLength.get() == -1) {
            // check if we can read the message length.
            if (buffer.remaining() < Integer.BYTES) {
                return null;
            } else {
                messageLength.set(buffer.getInt());
            }
        }

        if (buffer.remaining() < messageLength.get()) {
            return null;
        }

        long id = buffer.getLong();
        Version ipVersion = buffer.get() == 0x0 ? Version.INET : Version.INET6;
        byte[] octects = new byte[IpAddress.byteLength(ipVersion)];
        buffer.get(octects);
        IpAddress senderIp = IpAddress.valueOf(ipVersion, octects);
        int senderPort = buffer.getInt();
        int messageTypeByteLength = buffer.getInt();
        byte[] messageTypeBytes = new byte[messageTypeByteLength];
        buffer.get(messageTypeBytes);
        String messageType = new String(messageTypeBytes, Charsets.UTF_8);
        int payloadLength = buffer.getInt();
        byte[] payloadBytes = new byte[payloadLength];
        buffer.get(payloadBytes);

        // reset for next message
        messageLength.set(-1);

        return new DefaultMessage(id, new Endpoint(senderIp, senderPort), messageType, payloadBytes);
    }

    @Override
    protected void write(DefaultMessage message, ByteBuffer buffer) {
        Endpoint sender = message.sender();
        byte[] messageTypeBytes = message.type().getBytes(Charsets.UTF_8);
        IpAddress senderIp = sender.host();
        byte[] ipOctets = senderIp.toOctets();
        byte[] payload = message.payload();

        int messageLength = 21 + ipOctets.length + messageTypeBytes.length + payload.length;

        buffer.putInt(messageLength);

        buffer.putLong(message.id());

        if (senderIp.version() == Version.INET) {
            buffer.put((byte) 0x0);
        } else {
            buffer.put((byte) 0x1);
        }
        buffer.put(ipOctets);

        // write sender port
        buffer.putInt(sender.port());

        // write length of message type
        buffer.putInt(messageTypeBytes.length);

        // write message type bytes
        buffer.put(messageTypeBytes);

        // write payload length
        buffer.putInt(payload.length);

        // write payload.
        buffer.put(payload);
    }

    /**
     * Callback invoked when the stream is successfully connected.
     */
    public void connected() {
        connectFuture.complete(null);
    }

    /**
     * Callback invoked when the stream fails to connect.
     * @param cause failure cause
     */
    public void connectFailed(Throwable cause) {
        connectFuture.completeExceptionally(cause);
    }
}