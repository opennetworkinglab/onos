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

package org.onosproject.sdnip.config;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.config.Config;

/**
 * Configuration object for SDN-IP config.
 */
public class SdnIpConfig extends Config<ApplicationId> {

    public static final String CONFIG_KEY = "sdnip";

    public static final String ENCAPSULTATION = "encap";

    /**
     * Retrieves the encapsulation type set.
     *
     * @return the encapsulation type configured
     */
    public EncapsulationType encap() {
        EncapsulationType encapType = EncapsulationType.NONE;
        if (object.hasNonNull(ENCAPSULTATION)) {
            String encap = object.get(ENCAPSULTATION).asText();
            encapType = EncapsulationType.enumFromString(encap);
        }

        return encapType;
    }

    /**
     * Sets the encapsulation type used by SDN-IP.
     *
     * @param encap the encapsulation type
     */
    public void setEncap(EncapsulationType encap) {
        object.put(ENCAPSULTATION, encap.toString());
    }
}
