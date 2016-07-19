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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Instance ID type LCAF address class.
 * Instance ID type is defined in draft-ietf-lisp-lcaf-13
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-13#page-7
 *
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 2    | IID mask-len  |             4 + n             |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                         Instance ID                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |         Address  ...          |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 */
public class LispSegmentLcafAddress extends LispLcafAddress {

    private final LispAfiAddress address;
    private final int instanceId;

    /**
     * Initializes segment type LCAF address.
     *
     * @param idMaskLength Id mask length
     * @param instanceId instance id
     * @param address address
     */
    public LispSegmentLcafAddress(byte idMaskLength, int instanceId, LispAfiAddress address) {
        super(LispCanonicalAddressFormatEnum.SEGMENT, idMaskLength);
        this.address = address;
        this.instanceId = instanceId;
    }

    /**
     * Obtains address.
     *
     * @return address
     */
    public LispAfiAddress getAddress() {
        return address;
    }

    /**
     * Obtains instance id.
     *
     * @return instance id
     */
    public int getInstanceId() {
        return instanceId;
    }

    /**
     * Obtains id mask length.
     *
     * @return id mask length
     */
    public byte getIdMaskLength() {
        return reserved2;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, instanceId, reserved2);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispSegmentLcafAddress) {
            final LispSegmentLcafAddress other = (LispSegmentLcafAddress) obj;
            return Objects.equals(this.address, other.address) &&
                   Objects.equals(this.instanceId, other.instanceId) &&
                   Objects.equals(this.reserved2, other.reserved2);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("address", address)
                .add("instanceId", instanceId)
                .add("idMaskLength", reserved2)
                .toString();
    }
}
