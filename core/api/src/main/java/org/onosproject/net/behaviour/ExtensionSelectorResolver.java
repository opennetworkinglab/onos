/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

/**
 * Provides access to the extension selectors implemented by this driver.
 */
@Beta
public interface ExtensionSelectorResolver extends HandlerBehaviour {

    /**
     * Gets an extension selector instance of the specified type, if supported
     * by the driver.
     *
     * @param type type of extension to get
     * @return extension selector
     * @throws UnsupportedOperationException if the extension type is not
     * supported by this driver
     */
    ExtensionSelector getExtensionSelector(ExtensionSelectorType type);
}
