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

import java.util.List;

import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.openconfigplatform.platformcomponenttop.DefaultComponents;
import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.openconfigplatform.platformcomponenttop.components.Component;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20170708.openconfigplatformtransceiver.components.component.DefaultAugmentedOcPlatformComponent;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20170708.openconfigplatformtransceiver.porttransceivertop.DefaultTransceiver;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20170708.openconfigplatformtransceiver.porttransceivertop.transceiver.Config;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20170708.openconfigplatformtransceiver.porttransceivertop.transceiver.DefaultConfig;
import org.onosproject.yang.gen.v1.openconfigtransporttypes.rev20170816.openconfigtransporttypes.Eth100GbaseLr4;
import org.onosproject.yang.gen.v1.openconfigtransporttypes.rev20170816.openconfigtransporttypes.Qsfp28;
import org.onosproject.yang.model.DataNode;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

import static org.onosproject.odtn.utils.YangToolUtil.toDataNode;

/**
 * Utility methods dealing with OpenConfig transceiver.
 * <p>
 * Split into classes for the purpose of avoiding "Config" class collisions.
 */
@Beta
public abstract class Transceiver {

    public static List<DataNode> preconf(String componentName) {
        DefaultComponents components = new DefaultComponents();

        Component component = PlainPlatform.componentWithName(componentName);
        components.addToComponent(component);

        // augmented 'component' shim
        DefaultAugmentedOcPlatformComponent tcomponent = new DefaultAugmentedOcPlatformComponent();

        DefaultTransceiver transceiver = new DefaultTransceiver();

        Config configt = new DefaultConfig();
        // TODO make these configurable
        configt.formFactorPreconf(Qsfp28.class);
        configt.ethernetPmdPreconf(Eth100GbaseLr4.class);
        transceiver.config(configt);
        tcomponent.transceiver(transceiver);
        component.addAugmentation(tcomponent);

        return ImmutableList.of(toDataNode(components));
    }

}
