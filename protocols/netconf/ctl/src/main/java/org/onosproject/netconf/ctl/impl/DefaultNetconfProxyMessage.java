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

package org.onosproject.netconf.ctl.impl;

import com.google.common.collect.ImmutableList;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfProxyMessage;

import java.util.List;

/**
 * Default implementation of Netconf Proxy Message.
 */
public class DefaultNetconfProxyMessage implements NetconfProxyMessage {

    private final SubjectType subjectType;
    private final DeviceId deviceId;
    private final List<String> arguments;
    private final NodeId senderId;

    /**
     * Create new NetconfProxyMessage with provided informations.
     * @param subType Message subject type.
     * @param devId Device information that recieve message.
     * @param args Messages arguments.
     * @param nodeId nodeId of sender
     */
    public DefaultNetconfProxyMessage(SubjectType subType,
                                      DeviceId devId,
                                      List<String> args,
                                      NodeId nodeId) {
        subjectType = subType;
        deviceId = devId;
        arguments = args;
        senderId = nodeId;
    }


    @Override
    public SubjectType subjectType() {
        return subjectType;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public List<String> arguments() {
        return ImmutableList.copyOf(arguments);
    }

    @Override
    public NodeId senderId() {
        return senderId;
    }
}
