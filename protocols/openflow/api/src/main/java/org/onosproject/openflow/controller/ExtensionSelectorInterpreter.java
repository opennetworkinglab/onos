/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.openflow.controller;

import com.google.common.annotations.Beta;
import org.onosproject.codec.ExtensionSelectorCodec;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;

/**
 * Interprets extension selectors and converts them to/from OpenFlow objects.
 */
@Beta
public interface ExtensionSelectorInterpreter extends ExtensionSelectorCodec {

    /**
     * Returns true if the given extension selector is supported by this
     * driver.
     *
     * @param extensionSelectorType extension selector type
     * @return true if the instruction is supported, otherwise false
     */
    boolean supported(ExtensionSelectorType extensionSelectorType);

    /**
     * Maps an extension selector to an OpenFlow OXM.
     *
     * @param factory OpenFlow factory
     * @param extensionSelector extension selector
     * @return OpenFlow OXM
     */
    OFOxm<?> mapSelector(OFFactory factory, ExtensionSelector extensionSelector);

    /**
     * Maps an OpenFlow OXM to an extension selector.
     *
     * @param oxm OpenFlow OXM
     * @return extension selector
     */
    ExtensionSelector mapOxm(OFOxm<?> oxm);
}
