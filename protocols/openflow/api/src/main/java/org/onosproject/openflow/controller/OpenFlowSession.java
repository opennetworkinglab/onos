/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openflow.controller;

import java.util.Collections;

import org.projectfloodlight.openflow.protocol.OFMessage;

/**
 * Represents to OpenFlow session.
 */
public interface OpenFlowSession {

    /**
     * Returns session state.
     *
     * @return true if active.
     */
    boolean isActive();

    /**
     * Requests to close this OpenFlow session.
     */
    void closeSession();

    /**
     * Sends messages over this session.
     * @param msgs to send
     * @return true is messages were sent
     */
    boolean sendMsg(Iterable<OFMessage> msgs);

    /**
     * Sends message over this session.
     *
     * @param msg to send
     * @return true is messages were sent
     */
    default boolean sendMsg(OFMessage msg) {
        return sendMsg(Collections.singletonList(msg));
    }

    /**
     * Returns debug information about this session.
     *
     * @return debug information
     */
    CharSequence sessionInfo();

    /**
     * Add classifier to runtime store of classifiers.
     *
     * @param classifier the OpenFlow classifier to add
     */
    void addClassifier(OpenFlowClassifier classifier);

    /**
     * Remove classifier from runtime store of classifiers.
     *
     * @param classifier the OpenFlow classifier to remove
     */
    void removeClassifier(OpenFlowClassifier classifier);
}
