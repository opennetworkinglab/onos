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
package org.onosproject.odtn.utils.openconfig;

import static org.onosproject.odtn.utils.YangToolUtil.toDataNode;


import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminaldevicetop.DefaultTerminalDevice;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanneltop.logicalchannels.DefaultChannel;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanneltop.logicalchannels.channel.Config;
import org.onosproject.yang.gen.v1.openconfigtransporttypes.rev20170816.openconfigtransporttypes.adminstatetype.AdminStateTypeEnum;

import org.onosproject.yang.gen.v1.openconfigtransporttypes.rev20170816.openconfigtransporttypes.AdminStateType;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanneltop.DefaultLogicalChannels;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanneltop.logicalchannels.channel.DefaultConfig;
import org.onosproject.yang.model.DataNode;
import java.util.List;


/**
 * Utility methods dealing with OpenConfig logical-channel.
 * <p>
 * Split into classes for the purpose of avoiding "Config" class collisions.
 */
@Beta
public abstract class LogicalChannel {

    public static List<DataNode> enable(Long index,
                                        String description,
                                        boolean enable) {
        DefaultTerminalDevice terminalDevice = new DefaultTerminalDevice();
        DefaultLogicalChannels defaultLogicalChannels = new DefaultLogicalChannels();
        DefaultChannel channel = new DefaultChannel();
        AdminStateTypeEnum adminStateTypeEnum = null;
        if (enable) {
            adminStateTypeEnum = adminStateTypeEnum.ENABLED;
        } else {
            adminStateTypeEnum = adminStateTypeEnum.DISABLED;
        }
        AdminStateType adminStateType = new AdminStateType(adminStateTypeEnum);
        Config config = new DefaultConfig();
        config.description(description);
        config.adminState(adminStateType);
        channel.config(config);
        channel.index(index);
        defaultLogicalChannels.addToChannel(channel);
        terminalDevice.logicalChannels(defaultLogicalChannels);
        return ImmutableList.of(toDataNode(terminalDevice));
    }
}
