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
package org.onosproject.lisp.ctl.impl;

import org.onosproject.lisp.msg.protocols.LispEidRecord;
import org.onosproject.lisp.msg.protocols.LispMapRecord;

import java.util.List;

/**
 * A LISP database that stores EID-RLOC mapping information.
 */
public interface LispMappingDatabase {

    /**
     * Inserts a new EID-RLOC mapping record.
     *
     * @param eid           endpoint identifier
     * @param rloc          route locator record
     * @param proxyMapReply proxy map reply flag
     */
    void putMapRecord(LispEidRecord eid, LispMapRecord rloc, boolean proxyMapReply);

    /**
     * Removes an EID-RLOC mapping record with given endpoint identifier.
     *
     * @param eid endpoint identifier
     */
    void removeMapRecordByEid(LispEidRecord eid);

    /**
     * Removes all EID-RLOC mapping records.
     */
    void removeAllMapRecords();

    /**
     * Obtains an EID-RLOC mapping record in accordance with the proxy map reply
     * flag bit and EID record.
     *
     * @param eid           endpoint identifier record
     * @param proxyMapReply proxy map reply flag
     * @return an EID-RLOC mapping record
     */
    LispMapRecord getMapRecordByEidRecord(LispEidRecord eid, boolean proxyMapReply);

    /**
     * Obtains a collection of EID-RLOC mapping record in accordance with the
     * proxy map reply flag bit and EID record.
     *
     * @param eids          endpoint identifier records
     * @param proxyMapReply proxy map reply flag
     * @return a collection of EID-RLOC mapping records
     */
    List<LispMapRecord> getMapRecordByEidRecords(List<LispEidRecord> eids,
                                                 boolean proxyMapReply);



}
