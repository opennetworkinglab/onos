/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.optical;

import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import com.google.common.annotations.Beta;

/**
 * OCh port (Optical Channel).
 * Also referred to as a line side port (L-port) or narrow band port.
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)"
 */
@Beta
public interface OchPort extends ProjectedPort {

    /**
     * Returns ODU signal type.
     *
     * @return ODU signal type
     */
    public OduSignalType signalType();

    /**
     * Returns true if port is wavelength tunable.
     *
     * @return tunable wavelength capability
     */
    public boolean isTunable();

    /**
     * Returns OCh signal.
     *
     * @return OCh signal
     */
    public OchSignal lambda();

}
