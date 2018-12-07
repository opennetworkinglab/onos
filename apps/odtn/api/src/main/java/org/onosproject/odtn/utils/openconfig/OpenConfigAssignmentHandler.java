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

import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmenttop.logicalchannelassignments.DefaultAssignment;

import org.onosproject.yang.model.KeyLeaf;

/**
 * Utility class to deal with OPENCONFIG Assignment ModelObject & Annotation.
 */
public final class OpenConfigAssignmentHandler
        extends OpenConfigObjectHandler<DefaultAssignment> {

    private static final String OPENCONFIG_NAME = "assignment";
    private static final String KEY_LEAF_NAME = "index";
    private static final String NAME_SPACE = "http://openconfig.net/yang/terminal-device";

    /**
     * OpenConfigAssignmentHandler Constructor.
     *
     * @param keyValue String of target OPENCONFIG's key
     * @param parent OpenConfigLogicalChannelAssignmentsHandler of
     *               parent OPENCONFIG(logical-channel-assignments)
     */
    public OpenConfigAssignmentHandler(Integer keyValue,
                                       OpenConfigLogicalChannelAssignmentsHandler parent) {
        modelObject = new DefaultAssignment();
        modelObject.index(keyValue);
        setResourceId(OPENCONFIG_NAME, NAME_SPACE,
                      new KeyLeaf(KEY_LEAF_NAME, NAME_SPACE, keyValue),
                      parent.getResourceIdBuilder());
        annotatedNodeInfos = parent.getAnnotatedNodeInfoList();

        parent.addAssignment(this);
    }

    /**
     * Add Config to modelObject of target OPENCONFIG.
     *
     * @param config OpenConfigConfigOfAssignmentHandler having Config to be set for modelObject
     * @return OpenConfigAssignmentHandler of target OPENCONFIG
     */
    public OpenConfigAssignmentHandler addConfig(OpenConfigConfigOfAssignmentHandler config) {
        modelObject.config(config.getModelObject());
        return this;
    }
}
