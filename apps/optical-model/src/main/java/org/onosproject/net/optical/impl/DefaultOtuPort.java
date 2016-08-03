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
package org.onosproject.net.optical.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OtuPortHelper.stripHandledAnnotations;

import java.util.Objects;

import org.onosproject.net.Annotations;
import org.onosproject.net.OtuSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.optical.OtuPort;
import org.onosproject.net.utils.ForwardingPort;

import com.google.common.annotations.Beta;

/**
 * Implementation of OTU port (Optical channel Transport Unit).
 *
 */
@Beta
public class DefaultOtuPort extends ForwardingPort implements OtuPort {

    private final OtuSignalType signalType;

    /**
     * Creates an ODU client port.
     *
     * @param delegate      Port
     * @param signalType    OTU signal type
     */
    public DefaultOtuPort(Port delegate, OtuSignalType signalType) {
        super(delegate);
        this.signalType = checkNotNull(signalType);
    }

    @Override
    public Type type() {
        return Type.OTU;
    }

//    @Override
//    public long portSpeed() {
//        return signalType().bitRate();
//    }

    @Override
    public Annotations unhandledAnnotations() {
        return stripHandledAnnotations(super.annotations());
    }

    @Override
    public OtuSignalType signalType() {
        return signalType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                            signalType());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj != null && getClass() == obj.getClass()) {
            final DefaultOtuPort that = (DefaultOtuPort) obj;
            return super.toEqualsBuilder(that)
                    .append(this.signalType(), that.signalType())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return super.toStringHelper()
                .add("signalType", signalType())
                .add("annotations", unhandledAnnotations())
                .toString();
    }

}
