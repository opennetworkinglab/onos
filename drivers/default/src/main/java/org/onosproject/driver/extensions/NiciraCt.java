/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onosproject.driver.extensions.serializers.NiciraNatSerializer;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Nicira ct extension instruction.
 */
public class NiciraCt extends AbstractExtension implements ExtensionTreatment {

    private int flags;
    private long zoneSrc;
    private int zone;
    private short recircTable;
    private int alg;
    private List<ExtensionTreatment> nestedActions;
    private final KryoNamespace appKryo = new KryoNamespace.Builder()
                                                .register(HashMap.class)
                                                .register(ArrayList.class)
                                                .register(ExtensionTreatment.class)
                                                .register(new NiciraNatSerializer(),
                                                              NiciraNat.class)
                                                .build();

    /**
     * Creates a new nicicra ct instruction.
     */
    public NiciraCt() {
        flags = 0;
        zoneSrc = 0L;
        zone = 0;
        alg = 0;
        recircTable = 0xFF;
        nestedActions = new ArrayList<>();
    }

    /**
     * Creates a new nicicra ct instruction.
     * @param flags  zero or commit(0x01)
     * @param zoneSrc If 'zone_src' is nonzero, this specifies that the zone should be
     *                sourced from a field zone_src[ofs:ofs+nbits].
     * @param zone this is the union of zone_imm and zone_ofs_nbits
     *             If 'zone_src' is zero, then the value of 'zone_imm'
     *             will be used as the connection tracking zone
     * @param recircTable  Recirculate to a specific table or 0xff for no recirculation
     * @param alg  Well-known port number for the protocol, 0 indicates no ALG is required
     * @param actions a sequence of zero or more OpenFlow actions
     */
    public NiciraCt(int flags, long zoneSrc, int zone, short recircTable, int alg, List<ExtensionTreatment> actions) {
        this.flags = flags;
        this.zoneSrc = zoneSrc;
        this.zone = zone;
        this.recircTable = recircTable;
        this.alg = alg;
        this.nestedActions = actions;
    }

    @Override
    public ExtensionTreatmentType type() {
        return ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_CT.type();
    }

    /**
     * Get Nicira Conntrack flags.
     * @return flags
     */
    public int niciraCtFlags() {
        return flags;
    }

    /**
     * Get Nicira Conntrack zone.
     * @return zone
     */
    public int niciraCtZone() {
        return zone;
    }

    /**
     * Get Nicira Conntrack zone src.
     * @return zoneSrc
     */
    public long niciraCtZoneSrc() {
        return zoneSrc;
    }

    /**
     * Get Nicira Conntrack alg.
     * @return alg
     */
    public int niciraCtAlg() {
        return alg;
    }

    /**
     * Get Nicira Conntrack Recirc table.
     * @return recirc table
     */
    public short niciraCtRecircTable() {
        return recircTable;
    }

    /**
     * Get Nicira Conntrack Recirc table.
     * @return list extension treatment
     */
    public List<ExtensionTreatment> niciraCtNestActions() {
        return nestedActions;
    }

    @Override
    public void deserialize(byte[] data) {
        Map<String, Object> values = appKryo.deserialize(data);
        flags = (int) values.get("flags");
        zoneSrc = (long) values.get("zoneSrc");
        zone = (int) values.get("zone");
        recircTable = (short) values.get("recircTable");
        alg = (int) values.get("alg");
        nestedActions = (List) values.get("nestedActions");
    }

    @Override
    public byte[] serialize() {
        Map<String, Object> values = Maps.newHashMap();
        values.put("flags", flags);
        values.put("zoneSrc", zoneSrc);
        values.put("zone", zone);
        values.put("recircTable", recircTable);
        values.put("alg", alg);
        values.put("nestedActions", nestedActions);
        return appKryo.serialize(values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), flags, zone, zoneSrc, alg, recircTable, nestedActions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NiciraCt) {
            NiciraCt that = (NiciraCt) obj;
            return Objects.equals(flags, that.flags) &&
                    Objects.equals(zone, that.zone) &&
                    Objects.equals(zoneSrc, that.zoneSrc) &&
                    Objects.equals(alg, that.alg) &&
                    Objects.equals(recircTable, that.recircTable) &&
                    Objects.equals(nestedActions, that.nestedActions) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("flags", flags)
                .add("zoneSrc", zoneSrc)
                .add("zone", zone)
                .add("recircTable", recircTable)
                .add("alg", alg)
                .add("nestedActions", nestedActions)
                .toString();
    }
}