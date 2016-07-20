/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.iptopology.api.device;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip4Address;
import org.onosproject.iptopology.api.InterfaceIdentifier;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

/**
 * Default implementation of immutable Interface description.
 */
public class DefaultInterfaceDescription extends AbstractDescription
        implements InterfaceDescription {

    private final InterfaceIdentifier intfId;
    private final Ip4Address ipv4Address;
    private final Ip6Address ipv6Address;



    /**
     * Creates an interface description using the supplied information.
     *
     * @param intfId        interface identifier
     * @param ipv4Address   ipv4 address of an interface
     * @param ipv6Address   ipv6 address of an interface
     * @param annotations   optional key/value annotations map
     */
    public DefaultInterfaceDescription(InterfaceIdentifier intfId, Ip4Address ipv4Address,
                                  Ip6Address ipv6Address, SparseAnnotations...annotations) {
        super(annotations);
        this.intfId = intfId;
        this.ipv4Address = ipv4Address;
        this.ipv6Address = ipv6Address;
    }

    /**
     * Default constructor for serialization.
     */
    private DefaultInterfaceDescription() {
        this.intfId = null;
        this.ipv4Address = null;
        this.ipv6Address = null;
    }

    /**
     * Creates an interface description using the supplied information.
     *
     * @param base        InterfaceDescription to get basic information from
     * @param annotations optional key/value annotations map
     */
    public DefaultInterfaceDescription(InterfaceDescription base,
                                  SparseAnnotations annotations) {
        this(base.intfId(), base.ipv4Address(), base.ipv6Address(), annotations);
    }

    @Override
    public InterfaceIdentifier intfId() {
        return intfId;
    }

    @Override
    public Ip4Address ipv4Address() {
        return ipv4Address;
    }

    @Override
    public Ip6Address ipv6Address() {
        return ipv6Address; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("intfId", intfId)
                .add("ipv4Address", ipv4Address)
                .add("ipv6Address", ipv6Address)
                .add("annotations", annotations())
                .toString();
    }

}
