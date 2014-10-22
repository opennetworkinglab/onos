/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.cli.net;

import org.onlab.packet.Ethernet;

/**
 * Allowed values for Ethernet types.  Used by the CLI completer for
 * connectivity based intent L2 parameters.
 */
public enum EthType {
    /** ARP. */
    ARP(Ethernet.TYPE_ARP),
    /** RARP. */
    RARP(Ethernet.TYPE_RARP),
    /** IPV4. */
    IPV4(Ethernet.TYPE_IPV4),
    /** LLDP. */
    LLDP(Ethernet.TYPE_LLDP),
    /** BSN. */
    BSN(Ethernet.TYPE_BSN);

    private short value;

    /**
     * Constructs an EthType with the given value.
     *
     * @param value value to use when this EthType is seen.
     */
    private EthType(short value) {
        this.value = value;
    }

    /**
     * Gets the value to use for this EthType.
     *
     * @return short value to use for this EthType
     */
    public short value() {
        return this.value;
    }
}
