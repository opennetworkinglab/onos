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
package org.onosproject.driver.extensions;

import com.google.common.base.MoreObjects;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * OFDPA ACTSET_OUTPUT extension match.
 */
public class OfdpaMatchActsetOutput extends AbstractExtension implements ExtensionSelector {

    private PortNumber port;
    private static final KryoNamespace APPKRYO = new KryoNamespace.Builder()
            .register(PortNumber.class)
            .build();

    /**
     * OFDPA ACTSET_OUTPUT extension match.
     */
    public OfdpaMatchActsetOutput() {
        this.port = null;
    }

    /**
     * Constructs new ACTSET_OUTPUT match with given port number.
     *
     * @param port port number
     */
    public OfdpaMatchActsetOutput(PortNumber port) {
        checkNotNull(port);
        this.port = port;
    }

    /**
     * Gets the port number.
     *
     * @return the port number
     */
    public PortNumber port() {
        return port;
    }

    @Override
    public ExtensionSelectorType type() {
        return ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_ACTSET_OUTPUT.type();
    }

    @Override
    public byte[] serialize() {
        return APPKRYO.serialize(port);
    }

    @Override
    public void deserialize(byte[] data) {
        port = APPKRYO.deserialize(data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(port);
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !obj.getClass().equals(OfdpaMatchActsetOutput.class)) {
            return false;
        }

        OfdpaMatchActsetOutput that = (OfdpaMatchActsetOutput) obj;
        return port.equals(that.port());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("port", port)
                .toString();
    }
}
