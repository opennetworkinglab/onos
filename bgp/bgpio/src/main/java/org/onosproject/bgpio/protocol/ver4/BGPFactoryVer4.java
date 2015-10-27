/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgpio.protocol.ver4;

import org.onosproject.bgpio.protocol.BGPFactory;
import org.onosproject.bgpio.protocol.BGPKeepaliveMsg;
import org.onosproject.bgpio.protocol.BGPMessage;
import org.onosproject.bgpio.protocol.BGPMessageReader;
import org.onosproject.bgpio.protocol.BGPNotificationMsg;
import org.onosproject.bgpio.protocol.BGPOpenMsg;
import org.onosproject.bgpio.protocol.BGPVersion;

/**
 * Provides BGP Factory and returns builder classes for all objects and messages.
 */
public class BGPFactoryVer4 implements BGPFactory {

    public static final BGPFactoryVer4 INSTANCE = new BGPFactoryVer4();

    @Override
    public BGPOpenMsg.Builder openMessageBuilder() {
        return new BGPOpenMsgVer4.Builder();
    }

    @Override
    public BGPKeepaliveMsg.Builder keepaliveMessageBuilder() {
        return new BGPKeepaliveMsgVer4.Builder();
    }

    @Override
    public BGPNotificationMsg.Builder notificationMessageBuilder() {
        return new BGPNotificationMsgVer4.Builder();
    }

    @Override
    public BGPMessageReader<BGPMessage> getReader() {
        return BGPMessageVer4.READER;
    }

    @Override
    public BGPVersion getVersion() {
        return BGPVersion.BGP_4;
    }
}