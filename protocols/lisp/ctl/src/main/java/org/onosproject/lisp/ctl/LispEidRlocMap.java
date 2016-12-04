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
package org.onosproject.lisp.ctl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onlab.packet.IpPrefix;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

/**
 * A singleton class that stores EID-RLOC mapping information.
 */
public final class LispEidRlocMap {

    private ConcurrentMap<LispEidRecord, LispMapRecord> map = Maps.newConcurrentMap();

    /**
     * Obtains a singleton instance.
     *
     * @return singleton instance
     */
    public static LispEidRlocMap getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Inserts a new EID-RLOC mapping record.
     *
     * @param eid  endpoint identifier
     * @param rloc route locator record
     */
    public void insertMapRecord(LispEidRecord eid, LispMapRecord rloc) {
        map.putIfAbsent(eid, rloc);
    }

    /**
     * Removes an EID-RLOC mapping record with given endpoint identifier.
     *
     * @param eid endpoint identifier
     */
    public void removeMapRecordByEid(LispEidRecord eid) {
        map.remove(eid);
    }

    /**
     * Obtains an EID-RLOC mapping record with given EID record.
     *
     * @param eid endpoint identifier record
     * @return an EID-RLOC mapping record
     */
    public LispMapRecord getMapRecordByEidRecord(LispEidRecord eid) {

        for (LispEidRecord key : map.keySet()) {
            if (isInRange(key, eid)) {
                return map.get(key);
            }
        }

        return null;
    }

    /**
     * Obtains a collection of EID-RLOC mapping records with given EID records.
     *
     * @param eids endpoint identifier records
     * @return a collection of EID-RLOC mapping records
     */
    public List<LispMapRecord> getMapRecordByEidRecords(List<LispEidRecord> eids) {
        List<LispMapRecord> mapRecords = Lists.newArrayList();
        eids.forEach(eidRecord -> {
            LispMapRecord mapRecord = getMapRecordByEidRecord(eidRecord);
            if (mapRecord != null) {
                mapRecords.add(mapRecord);
            }
        });
        return ImmutableList.copyOf(mapRecords);
    }

    /**
     * Obtains an EID-RLOC mapping record with given EID address.
     *
     * @param address endpoint identifier address
     * @return an EID-RLOC mapping record
     */
    public LispMapRecord getMapRecordByEidAddress(LispAfiAddress address) {
        Optional<LispEidRecord> eidRecord =
                map.keySet().stream().filter(k -> k.getPrefix().equals(address)).findFirst();
        if (eidRecord.isPresent()) {
            return map.get(eidRecord);
        }

        return null;
    }

    /**
     * Prevents object instantiation from external.
     */
    private LispEidRlocMap() {
    }

    private static class SingletonHelper {
        private static final LispEidRlocMap INSTANCE = new LispEidRlocMap();
    }

    /**
     * Generates CIDR style string from EID record.
     *
     * @param eidRecord EID record
     * @return CIDR style string
     */
    private String cidrfy(LispEidRecord eidRecord) {
        StringBuilder sb = new StringBuilder();
        sb.append(eidRecord.getPrefix().toString());
        sb.append("/");
        sb.append(eidRecord.getMaskLength());
        return sb.toString();
    }

    /**
     * Checks whether the EID record is included in the given EID record.
     *
     * @param origin  the EID record to be compared
     * @param compare the EID record to compare
     * @return boolean result
     */
    private boolean isInRange(LispEidRecord origin, LispEidRecord compare) {

        IpPrefix originIpPrefix = IpPrefix.valueOf(cidrfy(origin));
        IpPrefix compareIpPrefix = IpPrefix.valueOf(cidrfy(compare));

        return originIpPrefix.contains(compareIpPrefix);
    }
}
