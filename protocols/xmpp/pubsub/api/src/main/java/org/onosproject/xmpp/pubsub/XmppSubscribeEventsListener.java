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

package org.onosproject.xmpp.pubsub;

import org.onosproject.xmpp.pubsub.model.XmppSubscribe;
import org.onosproject.xmpp.pubsub.model.XmppUnsubscribe;

/**
 * Allows for providers interested in XMPP Subscribe/Unsubscribe events to be notified.
 */
public interface XmppSubscribeEventsListener {

    /**
     * Method for handling incoming XMPP Subscribe message.
     *
     * @param subscribeEvent event related to incoming XMPP Subscribe message
     */
    void handleSubscribe(XmppSubscribe subscribeEvent);

    /**
     * Method for handling incoming XMPP Unsubscribe message.
     *
     * @param unsubscribeEvent event related to incoming XMPP Unsubscribe message
     */
    void handleUnsubscribe(XmppUnsubscribe unsubscribeEvent);

}
