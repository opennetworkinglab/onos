/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.netconf;

import java.util.Set;

/**
 * Abstract interface for the implementation of proxy message handler.
 */
public interface NetconfProxyMessageHandler {
    /**
     * Will decode the message on case basis and
     * call the actual method in Netconf Session implementation bound to secure transport.
     * @param proxyMessage incoming proxy message
     * @param <T> return type
     * @return the value returned by session call
     * @throws NetconfException netconf exception
     */
    <T> T handleIncomingMessage(NetconfProxyMessage proxyMessage) throws NetconfException;

    /**
     * Will decode the message on case basis and
     * call the actual method in Netconf Session implementation bound to secure transport.
     * @param replyMessage incoming reply message
     * @param <T> return type
     * @return the value returned by session call
     * @throws NetconfException netconf exception
     */
    <T> T handleReplyMessage(NetconfProxyMessage replyMessage) throws NetconfException;

    /**
     * Will decode the message on case basis and
     * call the actual method in Netconf Session implementation bound to secure transport.
     * @param proxyMessage incoming proxy message
     * @return the set value returned by session call
     * @throws NetconfException netconf exception
     */
    Set<String> handleIncomingSetMessage(NetconfProxyMessage proxyMessage) throws NetconfException;

    /**
     * Will decode the message on case basis and
     * call the actual method in Netconf Session implementation bound to secure transport.
     * @param replyMessage incoming proxy message
     * @return the set value returned by session call
     * @throws NetconfException netconf exception
     */
    Set<String> handleReplySetMessage(NetconfProxyMessage replyMessage) throws NetconfException;
}