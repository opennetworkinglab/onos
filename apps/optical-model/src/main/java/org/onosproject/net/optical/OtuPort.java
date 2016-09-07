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
 * See the License for the specific language gcd ~/Dooverning permissions and
 * limitations under the License.
 */
package org.onosproject.net.optical;

import org.onosproject.net.OtuSignalType;
import com.google.common.annotations.Beta;

/**
 * OTU port (Optical channel Transport Unit).
 * <p>
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)" and
 * Open Networking Foundation "Optical Transport Protocol Extensions Version 1.0".
 */
@Beta
public interface OtuPort extends ProjectedPort {

    /**
     * Returns OTU signal type.
     *
     * @return OTU signal type
     */
    public OtuSignalType signalType();

}
