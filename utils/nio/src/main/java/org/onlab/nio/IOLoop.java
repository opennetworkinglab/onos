/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.nio;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * I/O loop for driving inbound &amp; outbound {@link Message} transfer via
 * {@link MessageStream}.
 *
 * @param <M> message type
 * @param <S> message stream type
 */
public abstract class IOLoop<M extends Message, S extends MessageStream<M>>
        extends SelectorLoop {

    // Queue of requests for new message streams to enter the IO loop processing.
    private final Queue<NewStreamRequest> newStreamRequests = new ConcurrentLinkedQueue<>();

    // Carries information required for admitting a new message stream.
    private class NewStreamRequest {
        private final S stream;
        private final SelectableChannel channel;
        private final int op;

        public NewStreamRequest(S stream, SelectableChannel channel, int op) {
            this.stream = stream;
            this.channel = channel;
            this.op = op;
        }
    }

    // Set of message streams currently admitted into the IO loop.
    private final Set<MessageStream<M>> streams = new CopyOnWriteArraySet<>();

    /**
     * Creates an IO loop with the given selection timeout.
     *
     * @param timeout selection timeout in milliseconds
     * @throws IOException if the backing selector cannot be opened
     */
    public IOLoop(long timeout) throws IOException {
        super(timeout);
    }

    /**
     * Returns the number of message stream in custody of the loop.
     *
     * @return number of message streams
     */
    public int streamCount() {
        return streams.size();
    }

    /**
     * Creates a new message stream backed by the specified socket channel.
     *
     * @param byteChannel backing byte channel
     * @return newly created message stream
     */
    protected abstract S createStream(ByteChannel byteChannel);

    /**
     * Removes the specified message stream from the IO loop.
     *
     * @param stream message stream to remove
     */
    protected void removeStream(MessageStream<M> stream) {
        streams.remove(stream);
    }

    /**
     * Processes the list of messages extracted from the specified message
     * stream.
     *
     * @param messages non-empty list of received messages
     * @param stream   message stream from which the messages were extracted
     */
    protected abstract void processMessages(List<M> messages, MessageStream<M> stream);

    /**
     * Completes connection request pending on the given selection key.
     *
     * @param key selection key holding the pending connect operation.
     * @throws IOException when I/O exception of some sort has occurred
     */
    protected void connect(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        ch.finishConnect();
        if (key.isValid()) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    /**
     * Processes an IO operation pending on the specified key.
     *
     * @param key selection key holding the pending I/O operation.
     */
    protected void processKeyOperation(SelectionKey key) {
        @SuppressWarnings("unchecked")
        S stream = (S) key.attachment();

        try {
            // If the key is not valid, bail out.
            if (!key.isValid()) {
                stream.close();
                return;
            }

            // If there is a pending connect operation, complete it.
            if (key.isConnectable()) {
                try {
                    connect(key);
                } catch (IOException | IllegalStateException e) {
                    log.warn("Unable to complete connection", e);
                }
            }

            // If there is a read operation, slurp as much data as possible.
            if (key.isReadable()) {
                List<M> messages = stream.read();

                // No messages or failed flush imply disconnect; bail.
                if (messages == null || stream.hadError()) {
                    stream.close();
                    return;
                }

                // If there were any messages read, process them.
                if (!messages.isEmpty()) {
                    try {
                        processMessages(messages, stream);
                    } catch (RuntimeException e) {
                        onError(stream, e);
                    }
                }
            }

            // If there are pending writes, flush them
            if (key.isWritable()) {
                stream.flushIfPossible();
            }

            // If there were any issued flushing, close the stream.
            if (stream.hadError()) {
                stream.close();
            }

        } catch (CancelledKeyException e) {
            // Key was cancelled, so silently close the stream
            stream.close();
        } catch (IOException e) {
            if (!stream.isClosed() && !isResetByPeer(e)) {
                log.warn("Unable to process IO", e);
            }
            stream.close();
        }
    }

    // Indicates whether or not this exception is caused by 'reset by peer'.
    private boolean isResetByPeer(IOException e) {
        Throwable cause = e.getCause();
        return cause != null && cause instanceof IOException &&
                cause.getMessage().contains("reset by peer");
    }

    /**
     * Hook to allow intercept of any errors caused during message processing.
     * Default behaviour is to rethrow the error.
     *
     * @param stream message stream involved in the error
     * @param error  the runtime exception
     */
    protected void onError(S stream, RuntimeException error) {
        throw error;
    }

    /**
     * Admits a new message stream backed by the specified socket channel
     * with a pending accept operation.
     *
     * @param channel backing socket channel
     * @return newly accepted message stream
     */
    public S acceptStream(SocketChannel channel) {
        return createAndAdmit(channel, SelectionKey.OP_READ);
    }


    /**
     * Admits a new message stream backed by the specified socket channel
     * with a pending connect operation.
     *
     * @param channel backing socket channel
     * @return newly connected message stream
     */
    public S connectStream(SocketChannel channel) {
        return createAndAdmit(channel, SelectionKey.OP_CONNECT);
    }

    /**
     * Creates a new message stream backed by the specified socket channel
     * and admits it into the IO loop.
     *
     * @param channel socket channel
     * @param op      pending operations mask to be applied to the selection
     *                key as a set of initial interestedOps
     * @return newly created message stream
     */
    private synchronized S createAndAdmit(SocketChannel channel, int op) {
        S stream = createStream(channel);
        streams.add(stream);
        newStreamRequests.add(new NewStreamRequest(stream, channel, op));
        selector.wakeup();
        return stream;
    }

    /**
     * Safely admits new streams into the IO loop.
     */
    private void admitNewStreams() {
        Iterator<NewStreamRequest> it = newStreamRequests.iterator();
        while (isRunning() && it.hasNext()) {
            try {
                NewStreamRequest request = it.next();
                it.remove();
                SelectionKey key = request.channel.register(selector, request.op,
                                                            request.stream);
                request.stream.setKey(key);
            } catch (ClosedChannelException e) {
                log.warn("Unable to admit new message stream", e);
            }
        }
    }

    @Override
    protected void loop() throws IOException {
        notifyReady();

        // Keep going until told otherwise.
        while (isRunning()) {
            admitNewStreams();

            // Process flushes & write selects on all streams
            for (MessageStream<M> stream : streams) {
                stream.flushIfWriteNotPending();
            }

            // Select keys and process them.
            int count = selector.select(selectTimeout);
            if (count > 0 && isRunning()) {
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    processKeyOperation(key);
                }
            }
        }
    }

    /**
     * Prunes the registered streams by discarding any stale ones.
     *
     * @return number of remaining streams
     */
    public synchronized int pruneStaleStreams() {
        for (MessageStream<M> stream : streams) {
            if (stream.isStale()) {
                stream.close();
            }
        }
        return streams.size();
    }

}
