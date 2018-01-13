/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.xmpp.core;

import org.xmpp.packet.Message;


/**
 * Allows for providers interested in XMPP Message stanzas to be notified.
 */
public interface XmppMessageListener {

    /**
     * Handles incoming Message stanzas.
     *
     * @param message Message stanza to handle
     */
    void handleMessageStanza(Message message);

}
