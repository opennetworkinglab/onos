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
package org.onosproject.ospf.protocol.lsa.tlvtypes;

/**
 * Representation of an OSPF Opaque top level tlv types.
 */
public enum OpaqueTopLevelTlvTypes {

    ROUTER(1),
    LINK(2);

    private int value;

    /**
     * Creates an instance of Opaque top level tlv types.
     *
     * @param value opaque TLV value
     */
    OpaqueTopLevelTlvTypes(int value) {
        this.value = value;
    }

    /**
     * Gets the tlv type value.
     *
     * @return tlv type value
     */
    public int value() {
        return value;
    }

}
