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

import org.onosproject.xmpp.pubsub.model.XmppPublish;
import org.onosproject.xmpp.pubsub.model.XmppRetract;

/**
 * Allows for providers interested in XMPP Publish/Retract events to be notified.
 */
public interface XmppPublishEventsListener {

    /**
     * Method for handling incoming XMPP Publish message.
     *
     * @param publishEvent event related to incoming XMPP Publish message
     */
    void handlePublish(XmppPublish publishEvent);

    /**
     * Method for handling incoming XMPP Retract message.
     *
     * @param retractEvent event related to incoming XMPP Retract message
     */
    void handleRetract(XmppRetract retractEvent);

}
