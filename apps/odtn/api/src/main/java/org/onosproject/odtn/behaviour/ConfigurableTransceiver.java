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
package org.onosproject.odtn.behaviour;

import java.util.Collections;
import java.util.List;

import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.HandlerBehaviour;

import com.google.common.annotations.Beta;

/**
 * Behaviour for Transceiver.
 */
@Beta
public interface ConfigurableTransceiver extends HandlerBehaviour {

    /**
     * Generate configuration to enable/disable transceiver.
     *
     * @param client side port of transceiver to enable/disable
     * @param line side port of transceiver to enable/disable
     * @param enable or disable
     * @return XML documents (List to handle configuration with multiple-roots)
     */
    // return type and how component get specified are likely to change in future
    @Beta
    List<CharSequence> enable(PortNumber client, PortNumber line, boolean enable);

    // defined only for the purpose of test command without device.
    @Deprecated
    default List<CharSequence> enable(String name, boolean enable) {
        return Collections.emptyList();
    }

}
