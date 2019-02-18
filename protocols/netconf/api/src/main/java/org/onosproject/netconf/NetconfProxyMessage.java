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

import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;

import java.util.List;

/**
 * Interface representing a NETCONF proxy message.
 */
public interface NetconfProxyMessage {

    enum SubjectType {
        RPC,
        // FIXME in the final form there should only be (async) RPC
        // and REQUEST, REQUEST_SYNC should go away
        // once NetconfSession methods got cleaned up.
        REQUEST,
        REQUEST_SYNC,
        START_SUBSCRIPTION,
        END_SUBSCRIPTION,
        GET_SESSION_ID,
        GET_DEVICE_CAPABILITIES_SET,
        SET_ONOS_CAPABILITIES
    }

    /**
     * Returns the subject of the message.
     *
     * @return subject in enum subject type
     */
    NetconfProxyMessage.SubjectType subjectType();

    /**
     * Returns the device id of the device to which the message is intended.
     * @return device id
     */
    DeviceId deviceId();

    /**
     * Returns the arguments of the intended method call in order.
     * @return arguments
     */
    List<String> arguments();

    /**
     * Returns the node id of proxymessage sender.
     * @return NodeId
     */
    NodeId senderId();
}
