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

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

/**
 * Fixed-length message transfer buffer.
 */
public class TestMessageStream extends MessageStream<TestMessage> {

    private static final String E_WRONG_LEN = "Illegal message length: ";
    private static final long START_TAG = 0xfeedcafedeaddeedL;
    private static final long END_TAG = 0xbeadcafedeaddeedL;
    private static final int META_LENGTH = 40;

    private final int length;
    private boolean isStrict = true;

    public TestMessageStream(int length, ByteChannel ch, IOLoop<TestMessage, ?> loop) {
        super(loop, ch, 64 * 1024, 500);
        checkArgument(length >= META_LENGTH, "Length must be greater than header length of 40");
        this.length = length;
    }

    void setNonStrict() {
        isStrict = false;
    }

    @Override
    protected TestMessage read(ByteBuffer rb) {
        if (rb.remaining() < length) {
            return null;
        }

        long startTag = rb.getLong();
        if (isStrict) {
            checkState(startTag == START_TAG, "Incorrect message start");
        }

        long size = rb.getLong();
        long requestorTime = rb.getLong();
        long responderTime = rb.getLong();
        byte[] padding = padding();
        rb.get(padding);

        long endTag = rb.getLong();
        if (isStrict) {
            checkState(endTag == END_TAG, "Incorrect message end");
        }

        return new TestMessage((int) size, requestorTime, responderTime, padding);
    }

    @Override
    protected void write(TestMessage message, ByteBuffer wb) {
        if (message.length() != length) {
            throw new IllegalArgumentException(E_WRONG_LEN + message.length());
        }

        wb.putLong(START_TAG);
        wb.putLong(message.length());
        wb.putLong(message.requestorTime());
        wb.putLong(message.responderTime());
        wb.put(message.padding(), 0, length - META_LENGTH);
        wb.putLong(END_TAG);
    }

    public byte[] padding() {
        return new byte[length - META_LENGTH];
    }
}
