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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Test message for measuring rate and round-trip latency.
 */
public class TestMessage extends AbstractMessage {

    private final byte[] padding;

    private final long requestorTime;
    private final long responderTime;

    /**
     * Creates a new message with the specified data.
     *
     * @param requestorTime requester time
     * @param responderTime responder time
     * @param padding       message padding
     */
    TestMessage(int length, long requestorTime, long responderTime, byte[] padding) {
        this.length = length;
        this.requestorTime = requestorTime;
        this.responderTime = responderTime;
        this.padding = checkNotNull(padding, "Padding cannot be null");
    }

    public long requestorTime() {
        return requestorTime;
    }

    public long responderTime() {
        return responderTime;
    }

    public byte[] padding() {
        return padding;
    }

}
