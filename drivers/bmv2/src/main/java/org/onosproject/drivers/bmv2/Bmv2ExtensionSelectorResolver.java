/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.bmv2;

import org.onosproject.bmv2.api.runtime.Bmv2ExtensionSelector;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import static org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes.BMV2_MATCH_PARAMS;

/**
 * Implementation of the extension selector resolver behaviour for BMv2.
 */
public class Bmv2ExtensionSelectorResolver extends AbstractHandlerBehaviour implements ExtensionSelectorResolver {

    @Override
    public ExtensionSelector getExtensionSelector(ExtensionSelectorType type) {
        if (type.equals(BMV2_MATCH_PARAMS.type())) {
            return Bmv2ExtensionSelector.empty();
        }

        return null;
    }
}
