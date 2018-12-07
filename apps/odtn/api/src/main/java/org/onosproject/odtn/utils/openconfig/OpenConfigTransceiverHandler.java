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

import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20180515.openconfigplatformtransceiver.porttransceivertop.DefaultTransceiver;

/**
 * Utility class to deal with OPENCONFIG Transceiver ModelObject & Annotation.
 */
public final class OpenConfigTransceiverHandler
        extends OpenConfigObjectHandler<DefaultTransceiver> {

    private static final String OPENCONFIG_NAME = "transceiver";
    private static final String NAME_SPACE = "http://openconfig.net/yang/platform/transceiver";

    /**
     * OpenConfigTransceiverHandler Constructor.
     *
     * @param parent OpenConfigComponentHandler of parent OPENCONFIG(component)
     */
    public OpenConfigTransceiverHandler(OpenConfigComponentHandler parent) {
        modelObject = new DefaultTransceiver();
        setResourceId(OPENCONFIG_NAME, NAME_SPACE, null, parent.getResourceIdBuilder());
        annotatedNodeInfos = parent.getAnnotatedNodeInfoList();

        parent.addTransceiver(this);
    }

    /**
     * Add Config to modelObject of target OPENCONFIG.
     *
     * @param config OpenConfigConfigOfTransceiverHandler having Config to be set for modelObject
     * @return OpenConfigTransceiverHandler of target OPENCONFIG
     */
    public OpenConfigTransceiverHandler addConfig(OpenConfigConfigOfTransceiverHandler config) {
        modelObject.config(config.getModelObject());
        return this;
    }
}
