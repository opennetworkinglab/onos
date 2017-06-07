/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onlab.packet.IpPrefix;
import org.onosproject.lisp.ctl.impl.tree.IpConcurrentRadixTree;
import org.onosproject.lisp.msg.protocols.DefaultLispProxyMapRecord.DefaultMapWithProxyBuilder;
import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord;
import org.onosproject.lisp.msg.protocols.LispProxyMapRecord;
import org.slf4j.Logger;

import java.util.List;

import static org.onosproject.lisp.ctl.impl.util.LispMapUtil.cidrfy;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A radix tree based LISP mapping database.
 */
public final class LispRadixTreeDatabase implements LispMappingDatabase {

    private static final Logger log = getLogger(LispRadixTreeDatabase.class);

    private final IpConcurrentRadixTree<LispProxyMapRecord> radixTree =
            new IpConcurrentRadixTree<>();

    /**
     * Prevents object instantiation from external.
     */
    private LispRadixTreeDatabase() {
    }

    /**
     * Obtains a singleton instance.
     *
     * @return singleton instance
     */
    public static LispRadixTreeDatabase getInstance() {
        return SingletonHelper.INSTANCE;
    }

    @Override
    public void putMapRecord(LispEidRecord eid, LispMapRecord rloc,
                             boolean proxyMapReply) {
        final LispProxyMapRecord mapWithProxy = new DefaultMapWithProxyBuilder()
                .withMapRecord(rloc)
                .withIsProxyMapReply(proxyMapReply)
                .build();
        radixTree.put(getIpPrefix(eid), mapWithProxy);
        log.debug("Inserted a new map record for key {}", eid.toString());
    }

    @Override
    public void removeMapRecordByEid(LispEidRecord eid) {
        if (radixTree.remove(getIpPrefix(eid))) {
            log.debug("Removed a map record with key {}", eid.toString());
        }
    }

    @Override
    public void removeAllMapRecords() {
        radixTree.clear();
        log.debug("Clear all map records");
    }

    @Override
    public LispMapRecord getMapRecordByEidRecord(LispEidRecord eid,
                                                 boolean proxyMapReply) {
        final LispProxyMapRecord record =
                radixTree.getValueForClosestParentAddress(getIpPrefix(eid));
        if (record != null && record.isProxyMapReply() == proxyMapReply) {
            return record.getMapRecord();
        }
        return null;
    }

    @Override
    public List<LispMapRecord> getMapRecordByEidRecords(List<LispEidRecord> eids,
                                                        boolean proxyMapReply) {
        final List<LispMapRecord> mapRecords = Lists.newArrayList();
        eids.parallelStream().forEach(eidRecord -> {
            final LispMapRecord mapRecord =
                    getMapRecordByEidRecord(eidRecord, proxyMapReply);
            if (mapRecord != null) {
                mapRecords.add(mapRecord);
            }
        });
        return ImmutableList.copyOf(mapRecords);
    }

    /**
     * Prevents object instantiation from external.
     */
    private static final class SingletonHelper {
        private static final String ILLEGAL_ACCESS_MSG = "Should not instantiate this class.";
        private static final LispRadixTreeDatabase INSTANCE = new LispRadixTreeDatabase();

        private SingletonHelper() {
            throw new IllegalAccessError(ILLEGAL_ACCESS_MSG);
        }
    }

    /**
     * Obtains the IP prefix from LISP EID record.
     *
     * @param eidRecord LISP EID record
     * @return IP prefix object
     */
    private IpPrefix getIpPrefix(LispEidRecord eidRecord) {
        return IpPrefix.valueOf(cidrfy(eidRecord));
    }
}
