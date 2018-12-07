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

import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanneltop.logicalchannels.channel.DefaultConfig;

/**
 * Utility class to deal with OPENCONFIG Channel/Config ModelObject & Annotation.
 */
public final class OpenConfigConfigOfChannelHandler
        extends OpenConfigObjectHandler<DefaultConfig> {

    private static final String OPENCONFIG_NAME = "config";
    private static final String NAME_SPACE = "http://openconfig.net/yang/terminal-device";

    /**
     * OpenConfigConfigOfAssignmentHandler Constructor.
     *
     * @param parent OpenConfigChannelHandler of parent OPENCONFIG(channel)
     */
    public OpenConfigConfigOfChannelHandler(OpenConfigChannelHandler parent) {
        modelObject = new DefaultConfig();
        setResourceId(OPENCONFIG_NAME, NAME_SPACE, null, parent.getResourceIdBuilder());
        annotatedNodeInfos = parent.getAnnotatedNodeInfoList();

        parent.addConfig(this);
    }

    /**
     * Add child OPENCONFIG(index).
     *
     * @param index String to be set for modelObject
     * @return OpenConfigConfigOfChannelHandler of target OPENCONFIG
     */
    public OpenConfigConfigOfChannelHandler addIndex(Integer index) {
        modelObject.index(index);
        return this;
    }
}
