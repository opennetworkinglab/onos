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

import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmenttop.DefaultLogicalChannelAssignments;

/**
 * Utility class to deal with OPENCONFIG LogicalChannelAssignments ModelObject & Annotation.
 */
public final class OpenConfigLogicalChannelAssignmentsHandler
        extends OpenConfigObjectHandler<DefaultLogicalChannelAssignments> {

    private static final String OPENCONFIG_NAME = "logical-channel-assignments";
    private static final String NAME_SPACE = "http://openconfig.net/yang/terminal-device";

    /**
     * OpenConfigLogicalChannelAssignmentsHandler Constructor.
     *
     * @param parent OpenConfigChannelHandler of parent OPENCONFIG(channel)
     */
    public OpenConfigLogicalChannelAssignmentsHandler(OpenConfigChannelHandler parent) {
        modelObject = new DefaultLogicalChannelAssignments();
        setResourceId(OPENCONFIG_NAME, NAME_SPACE, null, parent.getResourceIdBuilder());
        annotatedNodeInfos = parent.getAnnotatedNodeInfoList();

        parent.addLogicalChannelAssignments(this);
    }

    /**
     * Add Assignment to modelObject of target OPENCONFIG.
     *
     * @param assignment OpenConfigAssignmentHandler having Assignment to be set for modelObject
     * @return OpenConfigLogicalChannelAssignmentsHandler of target OPENCONFIG
     */
    public OpenConfigLogicalChannelAssignmentsHandler addAssignment(
                                                      OpenConfigAssignmentHandler assignment) {
        modelObject.addToAssignment(assignment.getModelObject());
        return this;
    }
}
