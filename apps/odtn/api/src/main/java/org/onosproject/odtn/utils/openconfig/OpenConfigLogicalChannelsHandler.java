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

import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanneltop.DefaultLogicalChannels;

/**
 * Utility class to deal with OPENCONFIG LogicalChannels ModelObject & Annotation.
 */
public final class OpenConfigLogicalChannelsHandler
        extends OpenConfigObjectHandler<DefaultLogicalChannels> {

    private static final String OPENCONFIG_NAME = "logical-channels";
    private static final String NAME_SPACE = "http://openconfig.net/yang/terminal-device";

    /**
     * OpenConfigLogicalChannelsHandler Constructor.
     *
     * @param parent OpenConfigTerminalDeviceHandler of parent OPENCONFIG(terminal-device)
     */
    public OpenConfigLogicalChannelsHandler(OpenConfigTerminalDeviceHandler parent) {
        modelObject = new DefaultLogicalChannels();
        setResourceId(OPENCONFIG_NAME, NAME_SPACE, null, parent.getResourceIdBuilder());
        annotatedNodeInfos = parent.getAnnotatedNodeInfoList();

        parent.addLogicalChannels(this);
    }

    /**
     * Add Channel to modelObject of target OPENCONFIG.
     *
     * @param channel OpenConfigChannelHandler having Channel to be set for modelObject
     * @return OpenConfigLogicalChannelsHandler of target OPENCONFIG
     */
    public OpenConfigLogicalChannelsHandler addChannel(OpenConfigChannelHandler channel) {
        modelObject.addToChannel(channel.getModelObject());
        return this;
    }
}
