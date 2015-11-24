/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.driver.extensions;

import com.google.common.base.MoreObjects;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.instructions.AbstractExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.PortNumberSerializer;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nicira resubmit extension instruction.
 */
public class NiciraResubmit extends AbstractExtensionTreatment {

    private PortNumber inPort;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(new PortNumberSerializer(), PortNumber.class)
            .register(byte[].class)
            .build();

    /**
     * Creates a new resubmit instruction.
     */
    NiciraResubmit() {
        inPort = null;
    }

    /**
     * Creates a new resubmit instruction with a particular inPort.
     *
     * @param inPort in port number
     */
    public NiciraResubmit(PortNumber inPort) {
        checkNotNull(inPort);
        this.inPort = inPort;
    }

    /**
     * Gets the inPort.
     *
     * @return inPort
     */
    public PortNumber inPort() {
        return inPort;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT.type();
    }

    @Override
    public void deserialize(byte[] data) {
        inPort = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(inPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraResubmit) {
            NiciraResubmit that = (NiciraResubmit) obj;
            return Objects.equals(inPort, that.inPort);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("inPort", inPort)
                .toString();
    }
}
