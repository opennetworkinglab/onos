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

package org.onosproject.driver.extensions;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

import org.onlab.util.KryoNamespace;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.PortNumberSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nicira resubmit extension instruction.
 */
public class NiciraResubmit extends AbstractExtension implements ExtensionTreatment  {

    private static final short DEFAULT_TABLE_ID = 0;

    private PortNumber inPort;
    private short table;

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(new PortNumberSerializer(), PortNumber.class)
            .register(Map.class).register(HashMap.class)
            .register(byte[].class)
            .build("NiciraResubmit");

    /**
     * Creates a new resubmit instruction.
     */
    NiciraResubmit() {
        inPort = null;
        table = DEFAULT_TABLE_ID;
    }

    /**
     * Creates a new resubmit instruction with a particular inPort.
     *
     * @param inPort in port number
     */
    public NiciraResubmit(PortNumber inPort) {
        checkNotNull(inPort);
        this.inPort = inPort;
        this.table = DEFAULT_TABLE_ID;
    }

    /**
     * Gets the inPort.
     *
     * @return inPort
     */
    public PortNumber inPort() {
        return inPort;
    }

    /**
     * Gets the table.
     *
     * @return table
     */
    public short table() {
        return table;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT.type();
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        inPort = (PortNumber) values.get("inPort");
        table = (short) values.get("table");
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put("inPort", inPort);
        values.put("table", table);
        return appKryo.serialize(values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inPort, table);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraResubmit) {
            NiciraResubmit that = (NiciraResubmit) obj;
            return Objects.equals(inPort, that.inPort)
                          && (table == that.table());
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("inPort", inPort)
                .add("table", table)
                .toString();
    }
}
