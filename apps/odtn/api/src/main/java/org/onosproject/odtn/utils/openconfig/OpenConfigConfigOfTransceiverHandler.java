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

import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20180515.openconfigplatformtransceiver.porttransceivertop.transceiver.DefaultConfig;

/**
 * Utility class to deal with OPENCONFIG Transceiver/Config ModelObject & Annotation.
 */
public final class OpenConfigConfigOfTransceiverHandler
        extends OpenConfigObjectHandler<DefaultConfig> {

    private static final String OPENCONFIG_NAME = "config";
    private static final String NAME_SPACE = "http://openconfig.net/yang/platform/transceiver";

    /**
     * OpenConfigConfigOfTransceiverHandler Constructor.
     *
     * @param parent OpenConfigTransceiverHandler of parent OPENCONFIG(tranceiver)
     */
    public OpenConfigConfigOfTransceiverHandler(OpenConfigTransceiverHandler parent) {
        modelObject = new DefaultConfig();
        setResourceId(OPENCONFIG_NAME, NAME_SPACE, null, parent.getResourceIdBuilder());
        annotatedNodeInfos = parent.getAnnotatedNodeInfoList();

        parent.addConfig(this);
    }

    /**
     * Add enabled to modelObject of target OPENCONFIG.
     *
     * @param enabled boolean to be set for modelObject
     * @return OpenConfigConfigOfTransceiverHandler of target OPENCONFIG
     */
    public OpenConfigConfigOfTransceiverHandler addEnabled(boolean enabled) {
        modelObject.enabled(enabled);
        return this;
    }
}
