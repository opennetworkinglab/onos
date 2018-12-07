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

import org.onosproject.yang.gen.v1.openconfigplatform.rev20180603.openconfigplatform.platformcomponenttop.components.DefaultComponent;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20180515.openconfigplatformtransceiver.components.component.DefaultAugmentedOcPlatformComponent;

import org.onosproject.yang.model.KeyLeaf;

/**
 * Utility class to deal with OPENCONFIG Componet ModelObject & Annotation.
 */
public final class OpenConfigComponentHandler
        extends OpenConfigObjectHandler<DefaultComponent> {

    private static final String OPENCONFIG_NAME = "component";
    private static final String KEY_LEAF_NAME = "name";
    private static final String NAME_SPACE = "http://openconfig.net/yang/platform";

    private DefaultAugmentedOcPlatformComponent augmentedOcPlatformComponent;

    /**
     * OpenConfigComponentHandler Constructor.
     *
     * @param keyValue String of target OPENCONFIG's key
     * @param parent OpenConfigComponentsHandler of parent OPENCONFIG(components)
     */
    public OpenConfigComponentHandler(String keyValue, OpenConfigComponentsHandler parent) {
        modelObject = new DefaultComponent();
        modelObject.name(keyValue);
        setResourceId(OPENCONFIG_NAME, NAME_SPACE,
                      new KeyLeaf(KEY_LEAF_NAME, NAME_SPACE, keyValue),
                      parent.getResourceIdBuilder());
        annotatedNodeInfos = parent.getAnnotatedNodeInfoList();
        augmentedOcPlatformComponent = new DefaultAugmentedOcPlatformComponent();

        parent.addComponent(this);
    }

    /**
     * Add Transceiver to modelObject of target OPENCONFIG.
     *
     * @param transceiver OpenConfigTransceiverHandler having Transceiver to be set for modelObject
     * @return OpenConfigComponentHandler of target OPENCONFIG
     */
    public OpenConfigComponentHandler addTransceiver(OpenConfigTransceiverHandler transceiver) {
        augmentedOcPlatformComponent.transceiver(transceiver.getModelObject());
        modelObject.addAugmentation(augmentedOcPlatformComponent);
        return this;
    }

    /**
     * Add Config to modelObject of target OPENCONFIG.
     *
     * @param config OpenConfigConfigOfComponentHandler having Config to be set for modelObject
     * @return OpenConfigComponentHandler of target OPENCONFIG
     */
    public OpenConfigComponentHandler addConfig(OpenConfigConfigOfComponentHandler config) {
        modelObject.config(config.getModelObject());
        return this;
    }
}
