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
package org.onosproject.lisp.msg.types;

import org.onlab.packet.IpAddress;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * IPv6 address that is used by LISP Locator.
 */
public class LispIpv6Address extends LispIpAddress {

    /**
     * Initializes LISP locator's IPv6 address.
     *
     * @param address IP address
     */
    public LispIpv6Address(IpAddress address) {
        super(address, AddressFamilyIdentifierEnum.IP);
        checkArgument(address.isIp6());
    }
}
