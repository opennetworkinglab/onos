/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.driver.extensions;

import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.openflow.controller.ExtensionSelectorInterpreter;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;

/**
 * Interpreter for Nicira OpenFlow selector extensions.
 */
public class NiciraExtensionSelectorInterpreter
        extends AbstractHandlerBehaviour
        implements ExtensionSelectorInterpreter, ExtensionSelectorResolver {

    @Override
    public boolean supported(ExtensionSelectorType extensionSelectorType) {
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SI.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH1.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH2.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH3.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH4.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFOxm<?> mapSelector(OFFactory factory, ExtensionSelector extensionSelector) {
        ExtensionSelectorType type = extensionSelector.type();
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI.type())) {
            // TODO
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SI.type())) {
            // TODO
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH1.type())) {
            // TODO
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH2.type())) {
            // TODO
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH3.type())) {
            // TODO
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH4.type())) {
            // TODO
        }
        return null;
    }

    @Override
    public ExtensionSelector mapOxm(OFOxm<?> oxm) {
        return null;
    }

    @Override
    public ExtensionSelector getExtensionSelector(ExtensionSelectorType type) {
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI.type())) {
            return new NiciraMatchNshSpi();
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SI.type())) {
            return new NiciraMatchNshSi();
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH1.type())
                || type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH2.type())
                || type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH3.type())
                || type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH4.type())) {
            return new NiciraMatchNshContextHeader(type);
        }
        return null;
    }
}
