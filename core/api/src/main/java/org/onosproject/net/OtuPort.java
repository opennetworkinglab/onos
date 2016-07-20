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
package org.onosproject.net;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of OTU port (Optical channel Transport Unit).
 *
 * @deprecated in Goldeneye (1.6.0)
 */
@Deprecated
public class OtuPort extends DefaultPort {

    private final OtuSignalType signalType;

    /**
     * Creates an OTU port in the specified network element.
     *
     * @param element           parent network element
     * @param number            port number
     * @param isEnabled         port enabled state
     * @param signalType        OTU signal type
     * @param annotations       optional key/value annotations
     */
    public OtuPort(Element element, PortNumber number, boolean isEnabled,
            OtuSignalType signalType, Annotations... annotations) {
        super(element, number, isEnabled, Type.OTU, 0, annotations);
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
    public int hashCode() {
        return Objects.hash(number(), isEnabled(), type(), signalType, annotations());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OtuPort) {
            final OtuPort other = (OtuPort) obj;
            return Objects.equals(this.element().id(), other.element().id()) &&
                    Objects.equals(this.number(), other.number()) &&
                    Objects.equals(this.isEnabled(), other.isEnabled()) &&
                    Objects.equals(this.signalType, other.signalType) &&
                    Objects.equals(this.annotations(), other.annotations());
        }
        return false;
    }


    @Override
    public String toString() {
        return toStringHelper(this)
                .add("element", element().id())
                .add("number", number())
                .add("isEnabled", isEnabled())
                .add("type", type())
                .add("signalType", signalType)
                .toString();
    }

}