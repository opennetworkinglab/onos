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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//FIXME: Should be move out to test or app
/**
 * Message handler that echos the message back to the sender.
 */
public class EchoHandler implements MessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void handle(Message message) throws IOException {
        log.info("Received message. Echoing it back to the sender.");
        message.respond(message.payload());
    }
}
