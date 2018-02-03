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

import com.google.common.collect.ImmutableMap;
import org.xmpp.packet.PacketError;

/**
 * Constant values used across PubSub extension.
 */
public final class XmppPubSubConstants {

    public static final String PUBSUB_NAMESPACE = "http://jabber.org/protocol/pubsub";
    public static final String PUBSUB_EVENT_NS = "http://jabber.org/protocol/pubsub#event";
    public static final String PUBSUB_ERROR_NS = "http://jabber.org/protocol/pubsub#errors";
    public static final String PUBSUB_ELEMENT = "pubsub";
    public static final ImmutableMap<PubSubApplicationCondition, PacketError.Condition>
            APP_BASE_CONDITION_MAP = new ImmutableMap.Builder<PubSubApplicationCondition, PacketError.Condition>()
            .put(PubSubApplicationCondition.ITEM_NOT_FOUND, PacketError.Condition.item_not_found)
            .put(PubSubApplicationCondition.NOT_SUBSCRIBED, PacketError.Condition.unexpected_request)
            .build();

    public enum PubSubApplicationCondition {
        NOT_SUBSCRIBED,
        ITEM_NOT_FOUND
    }

    private XmppPubSubConstants() {
    }

    public enum Method {
        SUBSCRIBE,
        UNSUBSCRIBE,
        PUBLISH,
        RETRACT
    }

}
