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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.openconfigplatform.platformcomponenttop.DefaultComponents;
import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.openconfigplatform.platformcomponenttop.components.Component;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.components.component.DefaultAugmentedOcPlatformComponent;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminalopticalchanneltop.DefaultOpticalChannel;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminalopticalchanneltop.opticalchannel.Config;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.openconfigterminaldevice.terminalopticalchanneltop.opticalchannel.DefaultConfig;
import org.onosproject.yang.gen.v1.openconfigtransporttypes.rev20170816.openconfigtransporttypes.FrequencyType;
import org.onosproject.yang.model.DataNode;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

/**
 * Utility methods dealing with OpenConfig optical-channel.
 * <p>
 * Split into classes for the purpose of avoiding "Config" class collisions.
 */
@Beta
public abstract class OpticalChannel {

    public static List<DataNode> preconf(String componentName) {

        DefaultComponents components = new DefaultComponents();

        Component component = PlainPlatform.componentWithName(componentName);
        components.addToComponent(component);

        // augmented 'component' shim
        DefaultAugmentedOcPlatformComponent acomponent = new DefaultAugmentedOcPlatformComponent();

        DefaultOpticalChannel channel = new DefaultOpticalChannel();

        Config config = new DefaultConfig();
        // TODO make these configurable
        config.frequency(FrequencyType.of(BigInteger.valueOf(191500000)));
        config.targetOutputPower(BigDecimal.valueOf(0.0));

        channel.config(config);
        acomponent.opticalChannel(channel);
        component.addAugmentation(acomponent);

        return ImmutableList.of(toDataNode(components));
    }

}
