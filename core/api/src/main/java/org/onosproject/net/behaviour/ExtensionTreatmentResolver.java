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

package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

/**
 * Provides access to the extension treatments implemented by this driver.
 */
@Beta
public interface ExtensionTreatmentResolver extends HandlerBehaviour {

    /**
     * Gets an extension treatment instance of the specified type, if supported
     * by the driver.
     *
     * @param type type of extension to get
     * @return extension instruction
     * @throws UnsupportedOperationException if the extension type is not
     * supported by this driver
     */
    ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type);
}
