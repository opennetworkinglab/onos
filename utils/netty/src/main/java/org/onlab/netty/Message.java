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
package org.onlab.netty;

import java.io.IOException;

/**
 * A unit of communication.
 * Has a payload. Also supports a feature to respond back to the sender.
 */
public interface Message {

    /**
     * Returns the payload of this message.
     * @return message payload.
     */
    public byte[] payload();

    /**
     * Sends a reply back to the sender of this message.
     * @param data payload of the response.
     * @throws IOException if there is a communication error.
     */
    public void respond(byte[] data) throws IOException;
}
