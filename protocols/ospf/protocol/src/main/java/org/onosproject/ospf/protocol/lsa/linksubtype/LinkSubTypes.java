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
package org.onosproject.ospf.protocol.lsa.linksubtype;

/**
 * Representation of TE link sub types.
 */
public enum LinkSubTypes {
    LINK_TYPE(1),
    LINK_ID(2),
    LOCAL_INTERFACE_IP_ADDRESS(3),
    REMOTE_INTERFACE_IP_ADDRESS(4),
    TRAFFIC_ENGINEERING_METRIC(5),
    MAXIMUM_BANDWIDTH(6),
    MAXIMUM_RESERVABLE_BANDWIDTH(7),
    UNRESERVED_BANDWIDTH(8),
    ADMINISTRATIVE_GROUP(9);

    private int value;

    /**
     * Creates an instance of link sub types.
     *
     * @param value link sub type value
     */
    LinkSubTypes(int value) {
        this.value = value;
    }

    /**
     * Gets the link sub type value.
     *
     * @return link sub type value
     */
    public int value() {
        return value;
    }
}