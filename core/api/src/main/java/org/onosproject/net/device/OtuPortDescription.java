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
package org.onosproject.net.device;

import com.google.common.base.MoreObjects;

import org.onosproject.net.OtuSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;

/**
 * Default implementation of immutable OTU port description.
 *
 * @deprecated in Goldeneye (1.6.0)
 */
@Deprecated
public class OtuPortDescription extends DefaultPortDescription {

    private final OtuSignalType signalType;

    /**
     * Creates OTU port description based on the supplied information.
     *
     * @param number        port number
     * @param isEnabled     port enabled state
     * @param signalType    OTU signal type
     * @param annotations   optional key/value annotations map
     */
    public OtuPortDescription(PortNumber number, boolean isEnabled, OtuSignalType signalType,
            SparseAnnotations... annotations) {
        super(number, isEnabled, Port.Type.OTU, 0, annotations);
        this.signalType = signalType;
    }

    /**
     * Creates OTU port description based on the supplied information.
     *
     * @param base          PortDescription to get basic information from
     * @param signalType    OTU signal type
     * @param annotations   optional key/value annotations map
     */
    public OtuPortDescription(PortDescription base, OtuSignalType signalType,
            SparseAnnotations annotations) {
        super(base, annotations);
        this.signalType = signalType;
    }

    /**
     * Returns OTU signal type.
     *
     * @return OTU signal type
     */
    public OtuSignalType signalType() {
        return signalType;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("number", portNumber())
                .add("isEnabled", isEnabled())
                .add("type", type())
                .add("signalType", signalType)
                .toString();
    }

}