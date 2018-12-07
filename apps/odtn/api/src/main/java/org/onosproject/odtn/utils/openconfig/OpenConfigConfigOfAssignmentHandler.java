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

import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmentconfig.AssignmentTypeEnum;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminallogicalchanassignmenttop.logicalchannelassignments.assignment.DefaultConfig;

import java.math.BigDecimal;

/**
 * Utility class to deal with OPENCONFIG Assignment/Config ModelObject & Annotation.
 */
public final class OpenConfigConfigOfAssignmentHandler
        extends OpenConfigObjectHandler<DefaultConfig> {

    private static final String OPENCONFIG_NAME = "config";
    private static final String NAME_SPACE = "http://openconfig.net/yang/terminal-device";

    /**
     * OpenConfigConfigOfAssignmentHandler Constructor.
     *
     * @param parent OpenConfigAssignmentHandler of parent OPENCONFIG(assignment)
     */
    public OpenConfigConfigOfAssignmentHandler(OpenConfigAssignmentHandler parent) {
        modelObject = new DefaultConfig();
        setResourceId(OPENCONFIG_NAME, NAME_SPACE, null, parent.getResourceIdBuilder());
        annotatedNodeInfos = parent.getAnnotatedNodeInfoList();

        parent.addConfig(this);
    }

    /**
     * Add child OPENCONFIG(index).
     *
     * @param index Long to be set for modelObject
     * @return OpenConfigConfigOfAssignmentHandler of target OPENCONFIG
     */
    public OpenConfigConfigOfAssignmentHandler addIndex(Long index) {
        modelObject.index(index);
        return this;
    }

    /**
     * Add child OPENCONFIG(assignment-type).
     *
     * @param assignmentType AssignmentTypeEnum to be set for modelObject
     * @return OpenConfigConfigOfAssignmentHandler of target OPENCONFIG
     */
    public OpenConfigConfigOfAssignmentHandler addAssignmentType(
                                               AssignmentTypeEnum assignmentType) {
        modelObject.assignmentType(assignmentType);
        return this;
    }

    /**
     * Add child OPENCONFIG(logical-channel).
     *
     * @param logicalChannel String to be set for modelObject
     * @return OpenConfigConfigOfAssignmentHandler of target OPENCONFIG
     */
    public OpenConfigConfigOfAssignmentHandler addLogicalChannel(String logicalChannel) {
        modelObject.logicalChannel(logicalChannel);
        return this;
    }

    /**
     * Add child OPENCONFIG(allocation).
     *
     * @param allocation BigDecimal to be set for modelObject
     * @return OpenConfigConfigOfAssignmentHandler of target OPENCONFIG
     */
    public OpenConfigConfigOfAssignmentHandler addAllocation(BigDecimal allocation) {
        modelObject.allocation(allocation);
        return this;
    }
}
