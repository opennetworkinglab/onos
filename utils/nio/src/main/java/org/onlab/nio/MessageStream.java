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

import org.onlab.util.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.System.currentTimeMillis;
import static java.nio.ByteBuffer.allocateDirect;

/**
 * Bi-directional message stream for transferring messages to &amp; from the
 * network via two byte buffers.
 *
 * @param <M> message type
 */
public abstract class MessageStream<M extends Message> {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final IOLoop<M, ?> loop;
    private final ByteChannel channel;
    private final int maxIdleMillis;

    private final ByteBuffer inbound;
    private ByteBuffer outbound;
    private SelectionKey key;

    private volatile boolean closed = false;
    private volatile boolean writePending;
    private volatile boolean writeOccurred;

    private Exception ioError;
    private long lastActiveTime;

    private final Counter bytesIn = new Counter();
    private final Counter messagesIn = new Counter();
    private final Counter bytesOut = new Counter();
    private final Counter messagesOut = new Counter();

    /**
     * Creates a message stream associated with the specified IO loop and
     * backed by the given byte channel.
     *
     * @param loop          IO loop
     * @param byteChannel   backing byte channel
     * @param bufferSize    size of the backing byte buffers
     * @param maxIdleMillis maximum number of millis the stream can be idle
     *                      before it will be closed
     */
    protected MessageStream(IOLoop<M, ?> loop, ByteChannel byteChannel,
                            int bufferSize, int maxIdleMillis) {
        this.loop = checkNotNull(loop, "Loop cannot be null");
        this.channel = checkNotNull(byteChannel, "Byte channel cannot be null");

        checkArgument(maxIdleMillis > 0, "Idle time must be positive");
        this.maxIdleMillis = maxIdleMillis;

        inbound = allocateDirect(bufferSize);
        outbound = allocateDirect(bufferSize);
    }

    /**
     * Gets a single message from the specified byte buffer; this is
     * to be done without manipulating the buffer via flip, reset or clear.
     *
     * @param buffer byte buffer
     * @return read message or null if there are not enough bytes to read
     * a complete message
     */
    protected abstract M read(ByteBuffer buffer);

    /**
     * Puts the specified message into the specified byte buffer; this is
     * to be done without manipulating the buffer via flip, reset or clear.
     *
     * @param message message to be write into the buffer
     * @param buffer  byte buffer
     */
    protected abstract void write(M message, ByteBuffer buffer);

    /**
     * Closes the message buffer.
     */
    public void close() {
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
        }

        bytesIn.freeze();
        bytesOut.freeze();
        messagesIn.freeze();
        messagesOut.freeze();

        loop.removeStream(this);
        if (key != null) {
            try {
                key.cancel();
                key.channel().close();
            } catch (IOException e) {
                log.warn("Unable to close stream", e);
            }
        }
    }

    /**
     * Indicates whether this buffer has been closed.
     *
     * @return true if this stream has been closed
     */
    public synchronized boolean isClosed() {
        return closed;
    }

    /**
     * Returns the stream IO selection key.
     *
     * @return socket channel registration selection key
     */
    public SelectionKey key() {
        return key;
    }

    /**
     * Binds the selection key to be used for driving IO operations on the stream.
     *
     * @param key IO selection key
     */
    public void setKey(SelectionKey key) {
        this.key = key;
        this.lastActiveTime = currentTimeMillis();
    }

    /**
     * Returns the IO loop to which this stream is bound.
     *
     * @return I/O loop used to drive this stream
     */
    public IOLoop<M, ?> loop() {
        return loop;
    }

    /**
     * Indicates whether the any prior IO encountered an error.
     *
     * @return true if a write failed
     */
    public boolean hadError() {
        return ioError != null;
    }

    /**
     * Gets the prior IO error, if one occurred.
     *
     * @return IO error; null if none occurred
     */
    public Exception getError() {
        return ioError;
    }

    /**
     * Reads, without blocking, a list of messages from the stream.
     * The list will be empty if there were not messages pending.
     *
     * @return list of messages or null if backing channel has been closed
     * @throws IOException if messages could not be read
     */
    public List<M> read() throws IOException {
        try {
            int read = channel.read(inbound);
            if (read != -1) {
                // Read the messages one-by-one and add them to the list.
                List<M> messages = new ArrayList<>();
                M message;
                inbound.flip();
                while ((message = read(inbound)) != null) {
                    messages.add(message);
                    messagesIn.add(1);
                    bytesIn.add(message.length());
                }
                inbound.compact();

                // Mark the stream with current time to indicate liveness.
                lastActiveTime = currentTimeMillis();
                return messages;
            }
            return null;

        } catch (Exception e) {
            throw new IOException("Unable to read messages", e);
        }
    }

    /**
     * Writes the specified list of messages to the stream.
     *
     * @param messages list of messages to write
     * @throws IOException if error occurred while writing the data
     */
    public void write(List<M> messages) throws IOException {
        synchronized (this) {
            // First write all messages.
            for (M m : messages) {
                append(m);
            }
            flushUnlessAlreadyPlanningTo();
        }
    }

    /**
     * Writes the given message to the stream.
     *
     * @param message message to write
     * @throws IOException if error occurred while writing the data
     */
    public void write(M message) throws IOException {
        synchronized (this) {
            append(message);
            flushUnlessAlreadyPlanningTo();
        }
    }

    // Appends the specified message into the internal buffer, growing the
    // buffer if required.
    private void append(M message) {
        // If the buffer does not have sufficient length double it.
        while (outbound.remaining() < message.length()) {
            doubleSize();
        }
        write(message, outbound);
        messagesOut.add(1);
        bytesOut.add(message.length());
    }

    // Forces a flush, unless one is planned already.
    private void flushUnlessAlreadyPlanningTo() throws IOException {
        if (!writeOccurred && !writePending) {
            flush();
        }
    }

    /**
     * Flushes any pending writes.
     *
     * @throws IOException if flush failed
     */
    public void flush() throws IOException {
        synchronized (this) {
            if (!writeOccurred && !writePending) {
                outbound.flip();
                try {
                    channel.write(outbound);
                } catch (IOException e) {
                    if (!closed && !Objects.equals(e.getMessage(), "Broken pipe")) {
                        log.warn("Unable to write data", e);
                        ioError = e;
                    }
                }
                lastActiveTime = currentTimeMillis();
                writeOccurred = true;
                writePending = outbound.hasRemaining();
                outbound.compact();
            }
        }
    }

    /**
     * Indicates whether the stream has bytes to be written to the channel.
     *
     * @return true if there are bytes to be written
     */
    boolean isWritePending() {
        synchronized (this) {
            return writePending;
        }
    }


    /**
     * Indicates whether data has been written but not flushed yet.
     *
     * @return true if flush is required
     */
    boolean isFlushRequired() {
        synchronized (this) {
            return outbound.position() > 0;
        }
    }

    /**
     * Attempts to flush data, internal stream state and channel availability
     * permitting. Invoked by the driver I/O loop during handling of writable
     * selection key.
     * <p>
     * Resets the internal state flags {@code writeOccurred} and
     * {@code writePending}.
     * </p>
     * @throws IOException if implicit flush failed
     */
    void flushIfPossible() throws IOException {
        synchronized (this) {
            writePending = false;
            writeOccurred = false;
            if (outbound.position() > 0) {
                flush();
            }
        }
        key.interestOps(SelectionKey.OP_READ);
    }

    /**
     * Attempts to flush data, internal stream state and channel availability
     * permitting and if other writes are not pending. Invoked by the driver
     * I/O loop prior to entering select wait. Resets the internal
     * {@code writeOccurred} state flag.
     *
     * @throws IOException if implicit flush failed
     */
    void flushIfWriteNotPending() throws IOException {
        synchronized (this) {
            writeOccurred = false;
            if (!writePending && outbound.position() > 0) {
                flush();
            }
        }
        if (isWritePending()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    /**
     * Doubles the size of the outbound buffer.
     */
    private void doubleSize() {
        ByteBuffer newBuffer = allocateDirect(outbound.capacity() * 2);
        outbound.flip();
        newBuffer.put(outbound);
        outbound = newBuffer;
    }

    /**
     * Returns the maximum number of milliseconds the stream is allowed
     * without any read/write operations.
     *
     * @return number if millis of permissible idle time
     */
    protected int maxIdleMillis() {
        return maxIdleMillis;
    }


    /**
     * Returns true if the given stream has gone stale.
     *
     * @return true if the stream is stale
     */
    boolean isStale() {
        return currentTimeMillis() - lastActiveTime > maxIdleMillis() && key != null;
    }

    /**
     * Returns the inbound bytes counter.
     *
     * @return inbound bytes counter
     */
    public Counter bytesIn() {
        return bytesIn;
    }

    /**
     * Returns the outbound bytes counter.
     *
     * @return outbound bytes counter
     */
    public Counter bytesOut() {
        return bytesOut;
    }

    /**
     * Returns the inbound messages counter.
     *
     * @return inbound messages counter
     */
    public Counter messagesIn() {
        return messagesIn;
    }

    /**
     * Returns the outbound messages counter.
     *
     * @return outbound messages counter
     */
    public Counter messagesOut() {
        return messagesOut;
    }

}
