/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.lisp.ctl;

import org.onosproject.lisp.msg.protocols.LispMessage;

/**
 * Notifies providers about all LISP messages.
 */
public interface LispMessageListener {

    /**
     * Handles all incoming LISP messages.
     *
     * @param routerId the router where the message generated
     * @param msg      raw LISP message
     */
    void handleIncomingMessage(LispRouterId routerId, LispMessage msg);

    /**
     * Handles all outgoing LISP messages.
     *
     * @param routerId the router where the message to be sent
     * @param msg      raw LISP message
     */
    void handleOutgoingMessage(LispRouterId routerId, LispMessage msg);
}
