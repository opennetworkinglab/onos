/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgpio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;

import com.google.common.base.MoreObjects;

/**
 * Provides Implementation of Link Local/Remote IdentifiersTlv.
 */
public class LinkLocalRemoteIdentifiersTlv implements BgpValueType {
    public static final short TYPE = 258;
    private static final int LENGTH = 8;

    private final int linkLocalIdentifer;
    private final int linkRemoteIdentifer;

    /**
     * Constructor to initialize parameters.
     *
     * @param linkLocalIdentifer link local Identifer
     * @param linkRemoteIdentifer link remote Identifer
     */
    public LinkLocalRemoteIdentifiersTlv(int linkLocalIdentifer, int linkRemoteIdentifer) {
        this.linkLocalIdentifer = linkLocalIdentifer;
        this.linkRemoteIdentifer = linkRemoteIdentifer;
    }

    /**
     * Returns link remote Identifer.
     *
     * @return link remote Identifer
     */
    public int getLinkRemoteIdentifier() {
        return linkRemoteIdentifer;
    }

    /**
     * Returns link local Identifer.
     *
     * @return link local Identifer
     */
    public int getLinkLocalIdentifier() {
        return linkLocalIdentifer;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(linkLocalIdentifer, linkRemoteIdentifer);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LinkLocalRemoteIdentifiersTlv) {
            LinkLocalRemoteIdentifiersTlv other = (LinkLocalRemoteIdentifiersTlv) obj;
            return Objects.equals(this.linkLocalIdentifer, other.linkLocalIdentifer)
                    && Objects.equals(this.linkRemoteIdentifer, other.linkRemoteIdentifer);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeShort(TYPE);
        cb.writeShort(LENGTH);
        cb.writeInt(linkLocalIdentifer);
        cb.writeInt(linkRemoteIdentifer);
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of LinkLocalRemoteIdentifiersTlv.
     *
     * @param cb channelBuffer
     * @return object of LinkLocalRemoteIdentifiersTlv
     */
    public static LinkLocalRemoteIdentifiersTlv read(ChannelBuffer cb) {
        int linkLocalIdentifer = cb.readInt();
        int linkRemoteIdentifer = cb.readInt();
        return LinkLocalRemoteIdentifiersTlv.of(linkLocalIdentifer, linkRemoteIdentifer);
    }

    /**
     * Returns object of this class with specified link local identifer and link remote identifer.
     *
     * @param linkLocalIdentifer link local identifier
     * @param linkRemoteIdentifer link remote identifier
     * @return object of LinkLocalRemoteIdentifiersTlv
     */
    public static LinkLocalRemoteIdentifiersTlv of(final int linkLocalIdentifer, final int linkRemoteIdentifer) {
        return new LinkLocalRemoteIdentifiersTlv(linkLocalIdentifer, linkRemoteIdentifer);
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }
        int result = ((Integer) (this.linkLocalIdentifer))
                .compareTo((Integer) (((LinkLocalRemoteIdentifiersTlv) o).linkLocalIdentifer));
        if (result != 0) {
            return result;
        }
        return ((Integer) (this.linkRemoteIdentifer))
                .compareTo((Integer) (((LinkLocalRemoteIdentifiersTlv) o).linkRemoteIdentifer));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("TYPE", TYPE)
                .add("LENGTH", LENGTH)
                .add("linkLocalIdentifer", linkLocalIdentifer)
                .add("linkRemoteIdentifer", linkRemoteIdentifer)
                .toString();
    }
}