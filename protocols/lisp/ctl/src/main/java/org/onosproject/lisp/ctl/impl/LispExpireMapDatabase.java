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
package org.onosproject.lisp.ctl.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.lisp.ctl.impl.map.ExpireMap;
import org.onosproject.lisp.ctl.impl.map.ExpireHashMap;
import org.onosproject.lisp.msg.protocols.DefaultLispProxyMapRecord.DefaultMapWithProxyBuilder;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispProxyMapRecord;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.lisp.ctl.impl.util.LispMapUtil.isInRange;

/**
 * An expire map based LISP mapping database.
 * A singleton class that stores EID-RLOC mapping information.
 */
public final class LispExpireMapDatabase implements LispMappingDatabase {

    private static final long MINUTE_TO_MS_UNIT = 60 * 1000;

    private static final Logger log = getLogger(LispExpireMapDatabase.class);

    private final ExpireMap<LispEidRecord, LispProxyMapRecord> map = new ExpireHashMap<>();

    /**
     * Prevents object instantiation from external.
     */
    private LispExpireMapDatabase() {
    }

    /**
     * Obtains a singleton instance.
     *
     * @return singleton instance
     */
    public static LispExpireMapDatabase getInstance() {
        return SingletonHelper.INSTANCE;
    }

    @Override
    public void putMapRecord(LispEidRecord eid, LispMapRecord rloc,
                             boolean proxyMapReply) {
        LispProxyMapRecord mapWithProxy = new DefaultMapWithProxyBuilder()
                .withMapRecord(rloc)
                .withIsProxyMapReply(proxyMapReply)
                .build();
        map.put(eid, mapWithProxy, rloc.getRecordTtl() * MINUTE_TO_MS_UNIT);
    }

    /**
     * Returns the results whether a given EidRecord is contained in the map.
     *
     * @param eid endpoint identifier
     * @return the results whether a given EidRecord is contained in the map
     */
    public boolean hasEidRecord(LispEidRecord eid) {
        return map.containsKey(eid);
    }

    @Override
    public void removeMapRecordByEid(LispEidRecord eid) {
        map.remove(eid);
    }

    @Override
    public void removeAllMapRecords() {
        map.clear();
    }

    /**
     * Obtains all of the EID-RLOC mapping records.
     *
     * @return all of the EID-RLOC mapping records
     */
    public List<LispMapRecord> getAllMapRecords() {

        List<LispMapRecord> mapRecords = Lists.newArrayList();

        map.values().forEach(value -> mapRecords.add(value.getMapRecord()));

        return mapRecords;
    }

    @Override
    public LispMapRecord getMapRecordByEidRecord(LispEidRecord eid,
                                                 boolean proxyMapReply) {
        Optional<LispEidRecord> filteredEidRecord = map.keySet().parallelStream()
                .filter(k -> isInRange(k, eid)).findAny();
        if (filteredEidRecord.isPresent()) {
            LispProxyMapRecord record = map.get(filteredEidRecord.get());
            if (record != null && record.isProxyMapReply() == proxyMapReply) {
                return record.getMapRecord();
            }
        }

        return null;
    }

    @Override
    public List<LispMapRecord> getMapRecordByEidRecords(List<LispEidRecord> eids,
                                                        boolean proxyMapReply) {
        List<LispMapRecord> mapRecords = Lists.newArrayList();
        eids.forEach(eidRecord -> {
            LispMapRecord mapRecord =
                    getMapRecordByEidRecord(eidRecord, proxyMapReply);
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
                map.keySet().stream().filter(k ->
                        k.getPrefix().equals(address)).findFirst();
        return eidRecord.map(lispEidRecord ->
                map.get(lispEidRecord).getMapRecord()).orElse(null);
    }

    /**
     * Prevents object instantiation from external.
     */
    private static final class SingletonHelper {
        private static final String ILLEGAL_ACCESS_MSG = "Should not instantiate this class.";
        private static final LispExpireMapDatabase INSTANCE = new LispExpireMapDatabase();

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }
}
