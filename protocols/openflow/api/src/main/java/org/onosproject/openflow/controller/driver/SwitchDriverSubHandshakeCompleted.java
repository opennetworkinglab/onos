/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.openflow.controller.driver;

import org.projectfloodlight.openflow.protocol.OFMessage;


/**
 * Indicates that a message was passed to a switch driver's subhandshake
 * handling code but the driver has already completed the sub-handshake.
 *
 */
public class SwitchDriverSubHandshakeCompleted
        extends SwitchDriverSubHandshakeException {
    private static final long serialVersionUID = -8817822245846375995L;

    public SwitchDriverSubHandshakeCompleted(OFMessage m) {
        super("Sub-Handshake is already complete but received message "
              + m.getType());
    }
}
