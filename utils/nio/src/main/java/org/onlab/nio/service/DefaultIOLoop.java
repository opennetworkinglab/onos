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

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.function.Consumer;

import org.onlab.nio.IOLoop;
import org.onlab.nio.MessageStream;

/**
 * IOLoop for transporting DefaultMessages.
 */
public class DefaultIOLoop extends IOLoop<DefaultMessage, DefaultMessageStream> {

    public static final int SELECT_TIMEOUT_MILLIS = 500;
    private static final int MAX_IDLE_TIMEOUT_MILLIS = 1000;
    private static final int BUFFER_SIZE = 1024 * 1024;
    private final Consumer<DefaultMessage> consumer;

    public DefaultIOLoop(Consumer<DefaultMessage> consumer) throws IOException {
        this(SELECT_TIMEOUT_MILLIS, consumer);
    }

    public DefaultIOLoop(long timeout, Consumer<DefaultMessage> consumer) throws IOException {
        super(timeout);
        this.consumer = consumer;
    }

    @Override
    protected DefaultMessageStream createStream(ByteChannel byteChannel) {
        return new DefaultMessageStream(this, byteChannel, BUFFER_SIZE, MAX_IDLE_TIMEOUT_MILLIS);
    }

    @Override
    protected void processMessages(List<DefaultMessage> messages, MessageStream<DefaultMessage> stream) {
        messages.forEach(consumer);
    }

    @Override
    protected void connect(SelectionKey key) throws IOException {
        DefaultMessageStream stream = (DefaultMessageStream) key.attachment();
        try {
            super.connect(key);
            stream.connected();
        } catch (Exception e) {
            stream.connectFailed(e);
        }
    }
}