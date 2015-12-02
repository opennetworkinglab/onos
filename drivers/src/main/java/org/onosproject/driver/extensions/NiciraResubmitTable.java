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
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.store.serializers.PortNumberSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Nicira resubmit-table extension instruction.
 */
public class NiciraResubmitTable extends AbstractExtension implements
        ExtensionTreatment {

    //the list of the in port number(PortNumber) and the table(short)
    private List<Object> inPortAndTable = new ArrayList<Object>();

    private final KryoNamespace appKryo = new KryoNamespace.Builder()
            .register(ArrayList.class)
            .register(new PortNumberSerializer(), PortNumber.class)
            .register(short.class)
            .register(byte[].class)
            .build();

    /**
     * Creates a new resubmit-table instruction.
     */
    NiciraResubmitTable() {
        inPortAndTable = null;
    }

    /**
     * Creates a new resubmit-table instruction with a particular inPort and table.
     *
     * @param inPortAndTable the list of in port number and table
     */
    public NiciraResubmitTable(List<Object> inPortAndTable) {
        checkNotNull(inPortAndTable);
        this.inPortAndTable = inPortAndTable;
    }

    /**
     * Gets the inPortAndTable.
     *
     * @return inPortAndTable
     */
    public List<Object> inPortAndTable() {
        return inPortAndTable;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE.type();
    }

    @Override
    public void deserialize(byte[] data) {
        inPortAndTable = appKryo.deserialize(data);
    }

    @Override
    public byte[] serialize() {
        return appKryo.serialize(inPortAndTable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inPortAndTable);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraResubmitTable) {
            NiciraResubmitTable that = (NiciraResubmitTable) obj;
            return Objects.equals(inPortAndTable, that.inPortAndTable);

        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("inPortAndTable", inPortAndTable).toString();
    }
}
